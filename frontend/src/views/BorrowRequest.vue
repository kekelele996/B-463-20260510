<template>
  <div class="borrow-request">
    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-input
              v-model="searchQuery"
              placeholder="搜索资产名称"
              prefix-icon="Search"
              style="width: 200px; margin-right: 10px;"
              clearable
            />
            <el-button type="primary" icon="Plus" @click="openDialog">借用申请</el-button>
          </div>
          <el-button type="default" icon="Refresh" circle @click="fetchData" />
        </div>
      </template>

      <el-table :data="filteredData" style="width: 100%" stripe v-loading="loading" :row-class-name="rowClassName">
        <el-table-column prop="id" label="ID" width="70" sortable />
        <el-table-column prop="assetType" label="资产类型" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.assetType === 'equipment' ? '' : 'success'" size="small">
              {{ scope.row.assetType === 'equipment' ? '设备' : '车辆' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="assetName" label="资产名称" sortable />
        <el-table-column prop="applicantName" label="申请人" width="100" />
        <el-table-column prop="reason" label="借用原因" show-overflow-tooltip />
        <el-table-column prop="expectedReturnDate" label="预计归还" width="170" sortable>
          <template #default="scope">
            {{ formatDate(scope.row.expectedReturnDate) }}
          </template>
        </el-table-column>
        <el-table-column prop="actualReturnDate" label="实际归还" width="170">
          <template #default="scope">
            {{ scope.row.actualReturnDate ? formatDate(scope.row.actualReturnDate) : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row)" effect="dark">
              {{ getStatusLabel(scope.row) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="approverName" label="审批人" width="100">
          <template #default="scope">
            {{ scope.row.approverName || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="approveRemark" label="审批备注" show-overflow-tooltip>
          <template #default="scope">
            {{ scope.row.approveRemark || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" align="center" v-if="hasPermission('ROLE_ADMIN')">
          <template #default="scope">
            <el-button
              v-if="scope.row.status === 'pending'"
              size="small"
              type="success"
              plain
              @click="handleApprove(scope.row)"
            >通过</el-button>
            <el-button
              v-if="scope.row.status === 'pending'"
              size="small"
              type="danger"
              plain
              @click="handleReject(scope.row)"
            >驳回</el-button>
            <el-button
              v-if="scope.row.status === 'approved'"
              size="small"
              type="warning"
              plain
              @click="handleReturn(scope.row)"
            >归还</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" title="借用申请" width="550px">
      <el-form :model="form" label-width="100px" :rules="rules" ref="formRef">
        <el-form-item label="资产类型" prop="assetType">
          <el-select v-model="form.assetType" style="width: 100%" @change="onAssetTypeChange">
            <el-option label="设备" value="equipment" />
            <el-option label="车辆" value="vehicle" />
          </el-select>
        </el-form-item>
        <el-form-item label="选择资产" prop="assetId">
          <el-select v-model="form.assetId" style="width: 100%" placeholder="请选择资产">
            <el-option
              v-for="item in availableAssets"
              :key="item.id"
              :label="item.name || item.licensePlate"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="借用原因" prop="reason">
          <el-input v-model="form.reason" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="预计归还" prop="expectedReturnDate">
          <el-date-picker
            v-model="form.expectedReturnDate"
            type="datetime"
            placeholder="选择预计归还时间"
            style="width: 100%"
            value-format="YYYY-MM-DDTHH:mm:ss"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submit" :loading="submitting">提交</el-button>
        </span>
      </template>
    </el-dialog>

    <el-dialog v-model="approveDialogVisible" title="审批备注" width="400px">
      <el-input v-model="approveRemark" type="textarea" :rows="3" placeholder="请输入审批备注（可选）" />
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="approveDialogVisible = false">取消</el-button>
          <el-button :type="approveAction === 'approve' ? 'success' : 'danger'" @click="confirmApprove" :loading="submitting">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, reactive, computed } from 'vue'
import api from '../api'
import { ElMessage, ElMessageBox } from 'element-plus'

const tableData = ref([])
const loading = ref(false)
const searchQuery = ref('')
const dialogVisible = ref(false)
const approveDialogVisible = ref(false)
const submitting = ref(false)
const formRef = ref(null)
const availableAssets = ref([])
const approveRemark = ref('')
const approveAction = ref('')
const currentRow = ref(null)

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

const rules = {
  assetType: [{ required: true, message: '请选择资产类型', trigger: 'change' }],
  assetId: [{ required: true, message: '请选择资产', trigger: 'change' }],
  expectedReturnDate: [{ required: true, message: '请选择预计归还时间', trigger: 'change' }]
}

const filteredData = computed(() => {
  if (!searchQuery.value) return tableData.value
  return tableData.value.filter(item =>
    (item.assetName || '').toLowerCase().includes(searchQuery.value.toLowerCase())
  )
})

const isOverdue = (row) => {
  if (row.status !== 'approved') return false
  if (!row.expectedReturnDate) return false
  return new Date(row.expectedReturnDate) < new Date()
}

const rowClassName = ({ row }) => {
  return isOverdue(row) ? 'overdue-row' : ''
}

const getStatusLabel = (row) => {
  if (isOverdue(row)) return '超期未还'
  switch (row.status) {
    case 'pending': return '待审批'
    case 'approved': return '已批准'
    case 'rejected': return '已驳回'
    case 'returned': return '已归还'
    default: return row.status
  }
}

const getStatusType = (row) => {
  if (isOverdue(row)) return 'danger'
  switch (row.status) {
    case 'pending': return 'warning'
    case 'approved': return 'success'
    case 'rejected': return 'danger'
    case 'returned': return 'info'
    default: return ''
  }
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await api.get('/borrows')
    if (res.data.code === 200) {
      tableData.value = res.data.data
    }
  } finally {
    loading.value = false
  }
}

const onAssetTypeChange = async () => {
  form.assetId = null
  await fetchAvailableAssets()
}

const fetchAvailableAssets = async () => {
  try {
    const endpoint = form.assetType === 'equipment' ? '/equipments' : '/vehicles'
    const res = await api.get(endpoint)
    if (res.data.code === 200) {
      availableAssets.value = res.data.data.filter(item => item.status !== 'borrowed' && item.status !== 'maintenance')
    }
  } catch (e) {
    availableAssets.value = []
  }
}

const openDialog = async () => {
  dialogVisible.value = true
  Object.assign(form, { assetType: 'equipment', assetId: null, reason: '', expectedReturnDate: '' })
  await fetchAvailableAssets()
}

const submit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitting.value = true
      try {
        await api.post('/borrows', form)
        ElMessage.success('申请已提交')
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
  approveAction.value = 'approve'
  currentRow.value = row
  approveRemark.value = ''
  approveDialogVisible.value = true
}

const handleReject = (row) => {
  approveAction.value = 'reject'
  currentRow.value = row
  approveRemark.value = ''
  approveDialogVisible.value = true
}

const confirmApprove = async () => {
  submitting.value = true
  try {
    const endpoint = approveAction.value === 'approve'
      ? `/borrows/${currentRow.value.id}/approve`
      : `/borrows/${currentRow.value.id}/reject`
    await api.put(endpoint, { approveRemark: approveRemark.value })
    ElMessage.success(approveAction.value === 'approve' ? '已通过' : '已驳回')
    approveDialogVisible.value = false
    fetchData()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

const handleReturn = async (row) => {
  try {
    await ElMessageBox.confirm('确认归还该资产？', '归还确认', { type: 'warning' })
    await api.put(`/borrows/${row.id}/return`)
    ElMessage.success('已归还')
    fetchData()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e.response?.data?.message || '操作失败')
    }
  }
}

onMounted(fetchData)
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
:deep(.overdue-row) {
  background-color: #fef0f0 !important;
}
:deep(.overdue-row td) {
  color: #f56c6c !important;
}
</style>
