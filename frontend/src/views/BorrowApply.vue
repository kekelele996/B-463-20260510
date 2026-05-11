<template>
  <div class="borrow-apply">
    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <span>借用申请</span>
          <el-button type="primary" icon="Plus" @click="openDialog">申请借用</el-button>
        </div>
      </template>
      
      <el-table :data="tableData" style="width: 100%" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="applicantId" label="申请人ID" width="100" />
        <el-table-column prop="assetName" label="资产名称" />
        <el-table-column prop="reason" label="借用原因" show-overflow-tooltip />
        <el-table-column prop="borrowDate" label="借用日期" />
        <el-table-column prop="dueDate" label="应还日期">
          <template #default="scope">
            <span :class="{ 'overdue': isOverdue(scope.row) }">{{ scope.row.dueDate }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.status)">{{ getStatusLabel(scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="approverId" label="审批人ID" width="100" v-if="hasPermission('ROLE_ADMIN')" />
        <el-table-column prop="rejectReason" label="拒绝原因" show-overflow-tooltip v-if="hasPermission('ROLE_ADMIN')" />
        <el-table-column prop="createTime" label="申请时间" width="180" />
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" title="申请借用" width="500px">
      <el-form :model="form" label-width="100px" :rules="rules" ref="formRef">
        <el-form-item label="选择资产" prop="assetId">
          <el-select v-model="form.assetId" style="width: 100%" placeholder="请选择资产">
            <el-option 
              v-for="item in availableEquipments" 
              :key="item.id" 
              :label="item.name" 
              :value="item.id" 
            />
          </el-select>
        </el-form-item>
        <el-form-item label="借用日期" prop="borrowDate">
          <el-date-picker v-model="form.borrowDate" type="date" style="width: 100%" />
        </el-form-item>
        <el-form-item label="应还日期" prop="dueDate">
          <el-date-picker v-model="form.dueDate" type="date" style="width: 100%" />
        </el-form-item>
        <el-form-item label="借用原因" prop="reason">
          <el-input v-model="form.reason" type="textarea" :rows="3" placeholder="请输入借用原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitApply" :loading="submitting">提交申请</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, reactive } from 'vue'
import api from '../api'
import { ElMessage } from 'element-plus'

const tableData = ref([])
const equipments = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const submitting = ref(false)
const formRef = ref(null)

const user = JSON.parse(localStorage.getItem('user') || '{}')
const roles = user.roles || []

const hasPermission = (role) => {
  return roles.some(r => (typeof r === 'string' ? r === role : r.code === role))
}

const availableEquipments = ref([])

const formatDate = (date) => {
  if (!date) return ''
  const d = new Date(date)
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const form = reactive({
  assetId: null,
  borrowDate: '',
  dueDate: '',
  reason: ''
})

const rules = {
  assetId: [{ required: true, message: '请选择资产', trigger: 'change' }],
  borrowDate: [{ required: true, message: '请选择借用日期', trigger: 'change' }],
  dueDate: [{ required: true, message: '请选择应还日期', trigger: 'change' }]
}

const isOverdue = (row) => {
  if (row.status !== 'approved') return false
  const today = new Date()
  const dueDate = new Date(row.dueDate)
  return today > dueDate
}

const getStatusLabel = (status) => {
  switch (status) {
    case 'pending': return '待审批'
    case 'approved': return '已通过'
    case 'rejected': return '已拒绝'
    case 'returned': return '已归还'
    default: return status
  }
}

const getStatusType = (status) => {
  switch (status) {
    case 'pending': return 'warning'
    case 'approved': return 'success'
    case 'rejected': return 'danger'
    case 'returned': return 'info'
    default: return ''
  }
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await api.get('/borrow-records/my')
    if (res.data.code === 200) {
      tableData.value = res.data.data
    }
  } finally {
    loading.value = false
  }
}

const fetchEquipments = async () => {
  try {
    const res = await api.get('/equipments')
    if (res.data.code === 200) {
      equipments.value = res.data.data
      availableEquipments.value = equipments.value.filter(e => e.status !== 'borrowed')
    }
  } catch (e) {
    console.error(e)
  }
}

const openDialog = () => {
  fetchEquipments()
  dialogVisible.value = true
  Object.assign(form, {
    assetId: null,
    borrowDate: '',
    dueDate: '',
    reason: ''
  })
}

const submitApply = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitting.value = true
      try {
        const submitData = {
          ...form,
          borrowDate: formatDate(form.borrowDate),
          dueDate: formatDate(form.dueDate)
        }
        await api.post('/borrow-records', submitData)
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

onMounted(fetchData)
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.overdue {
  color: #f56c6c;
  font-weight: bold;
}
</style>
