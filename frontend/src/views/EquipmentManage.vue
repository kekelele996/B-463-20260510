<template>
  <div class="equipment-manage">
    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-input
              v-model="searchQuery"
              placeholder="搜索设备名称"
              prefix-icon="Search"
              style="width: 200px; margin-right: 10px;"
              clearable
            />
            <el-button type="primary" icon="Plus" @click="openDialog(false)" v-if="hasPermission('ROLE_ADMIN')">添加设备</el-button>
          </div>
          <el-button type="default" icon="Refresh" circle @click="fetchData" />
        </div>
      </template>
      
      <el-table :data="filteredData" style="width: 100%" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" sortable />
        <el-table-column prop="name" label="设备名称" sortable />
        <el-table-column prop="type" label="类型" />
        <el-table-column prop="status" label="状态">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.status)">{{ getStatusLabel(scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="maintenanceRecord" label="维护记录" show-overflow-tooltip />
        <el-table-column prop="lastMaintenanceBy" label="最后维护人" v-if="hasPermission('ROLE_ADMIN')" />
        <el-table-column label="操作" width="260" align="center">
          <template #default="scope">
            <el-button size="small" type="primary" plain icon="Edit" @click="openDialog(true, scope.row)">编辑</el-button>
            <el-button size="small" type="warning" plain @click="openBorrowDialog(scope.row)" v-if="scope.row.status !== 'borrowed'">借用</el-button>
            <el-popconfirm title="确定删除该设备吗？" @confirm="handleDelete(scope.row)" v-if="hasPermission('ROLE_ADMIN')">
              <template #reference>
                <el-button size="small" type="danger" plain icon="Delete">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑设备' : '添加设备'" width="500px">
      <el-form :model="form" label-width="80px" :rules="rules" ref="formRef">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" :disabled="!hasPermission('ROLE_ADMIN') && isEdit" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-input v-model="form.type" :disabled="!hasPermission('ROLE_ADMIN') && isEdit" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="form.status" style="width: 100%">
            <el-option label="在线" value="online" />
            <el-option label="离线" value="offline" />
            <el-option label="维护中" value="maintenance" />
            <el-option label="借出" value="borrowed" />
          </el-select>
        </el-form-item>
        <el-form-item label="维护记录" prop="maintenanceRecord">
          <el-input v-model="form.maintenanceRecord" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="save" :loading="submitting">确定</el-button>
        </span>
      </template>
    </el-dialog>

    <el-dialog v-model="borrowDialogVisible" title="借用设备" width="500px">
      <el-form :model="borrowForm" label-width="100px" :rules="borrowRules" ref="borrowFormRef">
        <el-form-item label="设备名称">
          <el-input :model-value="borrowForm.assetName" disabled />
        </el-form-item>
        <el-form-item label="借用原因" prop="reason">
          <el-input v-model="borrowForm.reason" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="预计归还" prop="expectedReturnDate">
          <el-date-picker
            v-model="borrowForm.expectedReturnDate"
            type="datetime"
            placeholder="选择预计归还时间"
            style="width: 100%"
            value-format="YYYY-MM-DDTHH:mm:ss"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="borrowDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitBorrow" :loading="borrowSubmitting">提交</el-button>
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
const searchQuery = ref('')
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref(null)
const borrowDialogVisible = ref(false)
const borrowSubmitting = ref(false)
const borrowFormRef = ref(null)

const user = JSON.parse(localStorage.getItem('user') || '{}')
const roles = user.roles || []

const hasPermission = (role) => {
  return roles.some(r => (typeof r === 'string' ? r === role : r.code === role))
}

const form = reactive({
  id: null,
  name: '',
  type: '',
  status: 'online',
  maintenanceRecord: ''
})

const rules = {
  name: [{ required: true, message: '请输入设备名称', trigger: 'blur' }],
  type: [{ required: true, message: '请输入设备类型', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

const borrowForm = reactive({
  assetId: null,
  assetName: '',
  reason: '',
  expectedReturnDate: ''
})

const borrowRules = {
  reason: [{ required: true, message: '请输入借用原因', trigger: 'blur' }],
  expectedReturnDate: [{ required: true, message: '请选择预计归还时间', trigger: 'change' }]
}

const filteredData = computed(() => {
  if (!searchQuery.value) return tableData.value
  return tableData.value.filter(item => 
    item.name.toLowerCase().includes(searchQuery.value.toLowerCase())
  )
})

const getStatusLabel = (status) => {
  switch (status) {
    case 'online': return '在线'
    case 'offline': return '离线'
    case 'maintenance': return '维护中'
    case 'borrowed': return '借出'
    default: return status
  }
}

const getStatusType = (status) => {
  switch (status) {
    case 'online': return 'success'
    case 'offline': return 'info'
    case 'maintenance': return 'warning'
    case 'borrowed': return 'danger'
    default: return ''
  }
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await api.get('/equipments')
    if (res.data.code === 200) {
      tableData.value = res.data.data
    }
  } finally {
    loading.value = false
  }
}

const openDialog = (edit, row) => {
  isEdit.value = edit
  dialogVisible.value = true
  if (edit) {
    Object.assign(form, row)
  } else {
    Object.assign(form, { id: null, name: '', type: '', status: 'online', maintenanceRecord: '' })
  }
}

const handleDelete = async (row) => {
  await api.delete(`/equipments/${row.id}`)
  ElMessage.success('删除成功')
  fetchData()
}

const save = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitting.value = true
      try {
        if (isEdit.value) {
          await api.put(`/equipments/${form.id}`, form)
        } else {
          await api.post('/equipments', form)
        }
        ElMessage.success('操作成功')
        dialogVisible.value = false
        fetchData()
      } catch (e) {
        ElMessage.error(e.response?.data?.message || '操作失败')
      } finally {
        submitting.value = false
      }
    }
  })
}

const openBorrowDialog = (row) => {
  Object.assign(borrowForm, { assetId: row.id, assetName: row.name, reason: '', expectedReturnDate: '' })
  borrowDialogVisible.value = true
}

const submitBorrow = async () => {
  if (!borrowFormRef.value) return
  await borrowFormRef.value.validate(async (valid) => {
    if (valid) {
      borrowSubmitting.value = true
      try {
        await api.post('/borrows', {
          assetType: 'equipment',
          assetId: borrowForm.assetId,
          reason: borrowForm.reason,
          expectedReturnDate: borrowForm.expectedReturnDate
        })
        ElMessage.success('借用申请已提交')
        borrowDialogVisible.value = false
      } catch (e) {
        ElMessage.error(e.response?.data?.message || '提交失败')
      } finally {
        borrowSubmitting.value = false
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
.header-left {
  display: flex;
  align-items: center;
}
</style>
