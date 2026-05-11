<template>
  <div class="borrow-approve">
    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <span>借用审批</span>
          <el-button type="default" icon="Refresh" circle @click="fetchData" />
        </div>
      </template>
      
      <el-table :data="tableData" style="width: 100%" stripe v-loading="loading" :row-class-name="tableRowClassName">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="applicantId" label="申请人ID" width="100" />
        <el-table-column prop="assetName" label="资产名称" />
        <el-table-column prop="reason" label="借用原因" show-overflow-tooltip />
        <el-table-column prop="borrowDate" label="借用日期" />
        <el-table-column prop="dueDate" label="应还日期" />
        <el-table-column prop="status" label="状态">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.status)">{{ getStatusLabel(scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="approverId" label="审批人ID" width="100" />
        <el-table-column prop="createTime" label="申请时间" width="180" />
        <el-table-column label="操作" width="280" align="center">
          <template #default="scope">
            <template v-if="scope.row.status === 'pending'">
              <el-button size="small" type="success" icon="Check" @click="handleApprove(scope.row)">通过</el-button>
              <el-button size="small" type="danger" icon="Close" @click="openRejectDialog(scope.row)">拒绝</el-button>
            </template>
            <template v-else-if="scope.row.status === 'approved'">
              <el-button size="small" type="primary" icon="RefreshRight" @click="handleReturn(scope.row)">确认归还</el-button>
            </template>
            <template v-else-if="scope.row.status === 'rejected'">
              <span>拒绝原因: {{ scope.row.rejectReason }}</span>
            </template>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="rejectDialogVisible" title="拒绝申请" width="400px">
      <el-form :model="rejectForm" label-width="80px">
        <el-form-item label="拒绝原因">
          <el-input v-model="rejectForm.rejectReason" type="textarea" :rows="3" placeholder="请输入拒绝原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="rejectDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="confirmReject" :loading="submitting">确认拒绝</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, reactive } from 'vue'
import api from '../api'
import { ElMessage, ElMessageBox } from 'element-plus'

const tableData = ref([])
const loading = ref(false)
const rejectDialogVisible = ref(false)
const submitting = ref(false)
const currentRecord = ref(null)

const rejectForm = reactive({
  rejectReason: ''
})

const isOverdue = (row) => {
  if (row.status !== 'approved') return false
  const today = new Date()
  const dueDate = new Date(row.dueDate)
  return today > dueDate
}

const tableRowClassName = ({ row }) => {
  if (isOverdue(row)) {
    return 'overdue-row'
  }
  return ''
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
    const res = await api.get('/borrow-records')
    if (res.data.code === 200) {
      tableData.value = res.data.data
    }
  } finally {
    loading.value = false
  }
}

const handleApprove = async (row) => {
  await ElMessageBox.confirm('确认通过该借用申请？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  try {
    await api.put(`/borrow-records/${row.id}/approve`)
    ElMessage.success('审批通过')
    fetchData()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '操作失败')
  }
}

const openRejectDialog = (row) => {
  currentRecord.value = row
  rejectForm.rejectReason = ''
  rejectDialogVisible.value = true
}

const confirmReject = async () => {
  if (!currentRecord.value) return
  submitting.value = true
  try {
    await api.put(`/borrow-records/${currentRecord.value.id}/reject`, rejectForm)
    ElMessage.success('已拒绝')
    rejectDialogVisible.value = false
    fetchData()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

const handleReturn = async (row) => {
  await ElMessageBox.confirm('确认资产已归还？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  try {
    await api.put(`/borrow-records/${row.id}/return`)
    ElMessage.success('归还确认成功')
    fetchData()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '操作失败')
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
:deep(.overdue-row) {
  background-color: #fef0f0 !important;
}
:deep(.overdue-row td) {
  color: #f56c6c !important;
  font-weight: bold;
}
</style>
