<template>
  <div class="vehicle-manage">
    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-input
              v-model="searchQuery"
              placeholder="搜索车牌号"
              prefix-icon="Search"
              style="width: 200px; margin-right: 10px;"
              clearable
            />
            <el-button type="primary" icon="Plus" @click="openDialog(false)" v-if="hasPermission('ROLE_ADMIN')">添加车辆</el-button>
          </div>
          <el-button type="default" icon="Refresh" circle @click="fetchData" />
        </div>
      </template>
      
      <el-table :data="filteredData" style="width: 100%" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" sortable />
        <el-table-column prop="licensePlate" label="车牌号" sortable />
        <el-table-column prop="model" label="型号" />
        <el-table-column prop="status" label="状态">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.status)">{{ getStatusLabel(scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="mileage" label="里程数 (km)" sortable />
        <el-table-column prop="maintenanceRecord" label="维护记录" show-overflow-tooltip />
        <el-table-column prop="lastMaintenanceBy" label="最后维护人" v-if="hasPermission('ROLE_ADMIN')" />
        <el-table-column label="操作" width="200" align="center">
          <template #default="scope">
            <el-button size="small" type="primary" plain icon="Edit" @click="openDialog(true, scope.row)">编辑</el-button>
            <el-popconfirm title="确定删除该车辆吗？" @confirm="handleDelete(scope.row)" v-if="hasPermission('ROLE_ADMIN')">
              <template #reference>
                <el-button size="small" type="danger" plain icon="Delete">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑车辆' : '添加车辆'" width="500px">
      <el-form :model="form" label-width="80px" :rules="rules" ref="formRef">
        <el-form-item label="车牌号" prop="licensePlate">
          <el-input v-model="form.licensePlate" :disabled="!hasPermission('ROLE_ADMIN') && isEdit" />
        </el-form-item>
        <el-form-item label="型号" prop="model">
          <el-input v-model="form.model" :disabled="!hasPermission('ROLE_ADMIN') && isEdit" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="form.status" style="width: 100%">
            <el-option label="行驶中" value="driving" />
            <el-option label="停泊中" value="parked" />
            <el-option label="维护中" value="maintenance" />
          </el-select>
        </el-form-item>
        <el-form-item label="里程数" prop="mileage">
          <el-input-number v-model="form.mileage" :min="0" style="width: 100%" />
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

const user = JSON.parse(localStorage.getItem('user') || '{}')
const roles = user.roles || []

const hasPermission = (role) => {
  return roles.some(r => (typeof r === 'string' ? r === role : r.code === role))
}

const form = reactive({
  id: null,
  licensePlate: '',
  model: '',
  status: 'parked',
  mileage: 0,
  maintenanceRecord: ''
})

const rules = {
  licensePlate: [{ required: true, message: '请输入车牌号', trigger: 'blur' }],
  model: [{ required: true, message: '请输入型号', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

const filteredData = computed(() => {
  if (!searchQuery.value) return tableData.value
  return tableData.value.filter(item => 
    item.licensePlate.toLowerCase().includes(searchQuery.value.toLowerCase())
  )
})

const getStatusLabel = (status) => {
  switch (status) {
    case 'driving': return '行驶中'
    case 'parked': return '停泊中'
    case 'maintenance': return '维护中'
    default: return status
  }
}

const getStatusType = (status) => {
  switch (status) {
    case 'driving': return 'success'
    case 'parked': return 'info'
    case 'maintenance': return 'warning'
    default: return ''
  }
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await api.get('/vehicles')
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
    Object.assign(form, { id: null, licensePlate: '', model: '', status: 'parked', mileage: 0, maintenanceRecord: '' })
  }
}

const handleDelete = async (row) => {
  await api.delete(`/vehicles/${row.id}`)
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
          await api.put(`/vehicles/${form.id}`, form)
        } else {
          await api.post('/vehicles', form)
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
