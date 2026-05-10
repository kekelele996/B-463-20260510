<template>
  <div class="borrow-application">
    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-select v-model="statusFilter" placeholder="筛选状态" clearable style="width: 150px; margin-right: 10px;">
              <el-option label="待审批" value="pending" />
              <el-option label="已通过" value="approved" />
              <el-option label="已拒绝" value="rejected" />
              <el-option label="已归还" value="returned" />
              <el-option label="已超期" value="overdue" />
            </el-select>
            <el-button type="primary" icon="Plus" @click="openDialog()">申请借用</el-button>
          </div>
          <el-button type="default" icon="Refresh" circle @click="fetchData" />
        </div>
      </template>
      
      <el-table :data="filteredData" style="width: 100%" stripe v-loading="loading" :row-class-name="tableRowClassName">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="assetType" label="资产类型" width="100">
          <template #default="scope">
            <el-tag type="primary" v-if="scope.row.assetType === 'equipment'">设备</el-tag>
            <el-tag type="success" v-else>车辆</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="assetName" label="资产名称" show-overflow-tooltip />
        <el-table-column prop="applicantName" label="申请人" width="100" />
        <el-table-column prop="reason" label="借用原因" show-overflow-tooltip />
        <el-table-column prop="expectedReturnDate" label="预计归还" width="120" />
        <el-table-column prop="actualReturnDate" label="实际归还" width="120" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.status)" :effect="scope.row.status === 'overdue' ? 'dark' : 'light'">
              {{ getStatusLabel(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="approverName" label="审批人" width="100" />
        <el-table-column label="操作" width="300" align="center">
          <template #default="scope">
            <template v-if="scope.row.status === 'pending' && hasPermission('ROLE_ADMIN')">
              <el-button size="small" type="success" icon="Check" @click="handleApprove(scope.row)">通过</el-button>
              <el-button size="small" type="warning" icon="Close" @click="handleReject(scope.row)">拒绝</el-button>
            </template>
            <template v-if="scope.row.status === 'approved' || scope.row.status === 'overdue'">
              <el-button size="small" type="primary" icon="RefreshRight" @click="handleReturn(scope.row)">归还</el-button>
            </template>
            <el-popconfirm title="确定删除该申请吗？" @confirm="handleDelete(scope.row)" v-if="hasPermission('ROLE_ADMIN')">
              <template #reference>
                <el-button size="small" type="danger" plain icon="Delete">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" title="申请借用" width="600px">
      <el-form :model="form" label-width="100px" :rules="rules" ref="formRef">
        <el-form-item label="资产类型" prop="assetType">
          <el-select v-model="form.assetType" style="width: 100%" @change="loadAssets">
            <el-option label="设备" value="equipment" />
            <el-option label="车辆" value="vehicle" />
          </el-select>
        </el-form-item>
        <el-form-item label="选择资产" prop="assetId">
          <el-select v-model="form.assetId" style="width: 100%" filterable placeholder="请选择资产">
            <el-option v-for="asset in availableAssets" :key="asset.id" :label="asset.displayName" :value="asset.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="借用原因" prop="reason">
          <el-input v-model="form.reason" type="textarea" :rows="3" placeholder="请详细说明借用原因" />
        </el-form-item>
        <el-form-item label="预计归还" prop="expectedReturnDate">
          <el-date-picker
            v-model="form.expectedReturnDate"
            type="date"
            placeholder="选择日期"
            style="width: 100%"
            :disabled-date="disabledDate"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="save" :loading="submitting">提交申请</el-button>
        </span>
      </template>
    </el-dialog>

    <el-dialog v-model="approveDialogVisible" title="审批" width="500px">
      <el-form :model="approveForm" label-width="100px">
        <el-form-item label="审批意见">
          <el-input v-model="approveForm.reason" type="textarea" :rows="3" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="approveDialogVisible = false">取消</el-button>
          <el-button type="success" @click="submitApprove">确认通过</el-button>
        </span>
      </template>
    </el-dialog>

    <el-dialog v-model="rejectDialogVisible" title="拒绝申请" width="500px">
      <el-form :model="rejectForm" label-width="100px">
        <el-form-item label="拒绝原因">
          <el-input v-model="rejectForm.reason" type="textarea" :rows="3" placeholder="请说明拒绝原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="rejectDialogVisible = false">取消</el-button>
          <el-button type="warning" @click="submitReject">确认拒绝</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, reactive, computed } from 'vue'
import api from '../api'
import { ElMessage } from 'element-plus'

const tableData = ref([])
const loading = ref(false)
const statusFilter = ref('')
const dialogVisible = ref(false)
const approveDialogVisible = ref(false)
const rejectDialogVisible = ref(false)
const submitting = ref(false)
const formRef = ref(null)

const equipments = ref([])
const vehicles = ref([])
const availableAssets = ref([])
const currentApp = ref(null)

const user = JSON.parse(localStorage.getItem('user') || '{}')
const roles = user.roles || []

const hasPermission = (role) => {
  return roles.some(r => (typeof r === 'string' ? r === role : r.code === role))
}

const form = reactive({
  assetType: 'equipment',
  assetId: null,
  reason: '',
  expectedReturnDate: ''
})

const approveForm = reactive({ reason: '' })
const rejectForm = reactive({ reason: '' })

const rules = {
  assetType: [{ required: true, message: '请选择资产类型', trigger: 'change' }],
  assetId: [{ required: true, message: '请选择资产', trigger: 'change' }],
  reason: [{ required: true, message: '请填写借用原因', trigger: 'blur' }],
  expectedReturnDate: [{ required: true, message: '请选择预计归还日期', trigger: 'change' }]
}

const disabledDate = (time) => {
  return time.getTime() < Date.now() - 8.64e7
}

const filteredData = computed(() => {
  if (!statusFilter.value) return tableData.value
  return tableData.value.filter(item => item.status === statusFilter.value)
})

const getStatusLabel = (status) => {
  switch (status) {
    case 'pending': return '待审批'
    case 'approved': return '已通过'
    case 'rejected': return '已拒绝'
    case 'returned': return '已归还'
    case 'overdue': return '已超期'
    case 'borrowed': return '已借出'
    default: return status
  }
}

const getStatusType = (status) => {
  switch (status) {
    case 'pending': return 'warning'
    case 'approved': return 'success'
    case 'rejected': return 'danger'
    case 'returned': return 'info'
    case 'overdue': return 'danger'
    case 'borrowed': return 'primary'
    default: return ''
  }
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await api.get('/borrow-applications')
    if (res.data.code === 200) {
      tableData.value = res.data.data
    }
  } finally {
    loading.value = false
  }
}

const loadEquipments = async () => {
  try {
    const res = await api.get('/equipments')
    if (res.data.code === 200) {
      equipments.value = res.data.data
        .filter(e => e.status !== 'borrowed')
        .map(e => ({ ...e, displayName: e.name }))
    }
  } catch (e) {
    console.error(e)
  }
}

const loadVehicles = async () => {
  try {
    const res = await api.get('/vehicles')
    if (res.data.code === 200) {
      vehicles.value = res.data.data
        .filter(v => v.status !== 'borrowed')
        .map(v => ({ ...v, displayName: v.licensePlate + ' - ' + v.model }))
    }
  } catch (e) {
    console.error(e)
  }
}

const tableRowClassName = ({ row }) => {
  if (row.status === 'overdue') {
    return 'overdue-row'
  }
  return ''
}

const loadAssets = () => {
  if (form.assetType === 'equipment') {
    availableAssets.value = equipments.value
  } else {
    availableAssets.value = vehicles.value
  }
  form.assetId = null
}

const openDialog = () => {
  form.assetType = 'equipment'
  form.assetId = null
  form.reason = ''
  form.expectedReturnDate = ''
  loadAssets()
  dialogVisible.value = true
}

const save = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitting.value = true
      try {
        await api.post('/borrow-applications', form)
        ElMessage.success('申请提交成功')
        dialogVisible.value = false
        fetchData()
      } catch (e) {
        ElMessage.error(e.response?.data?.message || '提交失败')
      } finally {
        submitting.value = false
      }
    }
  })
}

const handleApprove = (row) => {
  currentApp.value = row
  approveForm.reason = ''
  approveDialogVisible.value = true
}

const submitApprove = async () => {
  try {
    await api.put(`/borrow-applications/${currentApp.value.id}/approve`, { reason: approveForm.reason })
    ElMessage.success('审批通过')
    approveDialogVisible.value = false
    fetchData()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '操作失败')
  }
}

const handleReject = (row) => {
  currentApp.value = row
  rejectForm.reason = ''
  rejectDialogVisible.value = true
}

const submitReject = async () => {
  try {
    await api.put(`/borrow-applications/${currentApp.value.id}/reject`, { reason: rejectForm.reason })
    ElMessage.success('已拒绝')
    rejectDialogVisible.value = false
    fetchData()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '操作失败')
  }
}

const handleReturn = async (row) => {
  try {
    await api.put(`/borrow-applications/${row.id}/return`)
    ElMessage.success('归还成功')
    fetchData()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '操作失败')
  }
}

const handleDelete = async (row) => {
  try {
    await api.delete(`/borrow-applications/${row.id}`)
    ElMessage.success('删除成功')
    fetchData()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '操作失败')
  }
}

onMounted(() => {
  fetchData()
  loadEquipments()
  loadVehicles()
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.header-left {
  display: flex;
  align-items: center;
}
</style>

<style>
.overdue-row {
  background-color: #fef0f0 !important;
}
.overdue-row td {
  background-color: #fef0f0 !important;
  color: #f56c6c !important;
}
.overdue-row:hover td {
  background-color: #fde2e2 !important;
}
</style>
