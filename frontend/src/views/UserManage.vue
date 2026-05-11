<template>
  <div class="user-manage">
    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-input
              v-model="searchQuery"
              placeholder="搜索用户名"
              prefix-icon="Search"
              style="width: 200px; margin-right: 10px;"
              clearable
            />
            <el-button type="primary" icon="Plus" @click="openDialog(false)">添加用户</el-button>
          </div>
          <el-button type="default" icon="Refresh" circle @click="fetchData" />
        </div>
      </template>
      
      <el-table :data="filteredData" style="width: 100%" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" sortable />
        <el-table-column prop="username" label="用户名" sortable />
        <el-table-column label="角色">
          <template #default="scope">
            <el-tag 
              v-for="role in scope.row.roles" 
              :key="role.id"
              :type="role.code === 'ROLE_ADMIN' ? 'danger' : 'primary'" 
              effect="light"
              style="margin-right: 5px"
            >
              {{ role.name }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" align="center">
          <template #default="scope">
            <el-button size="small" type="primary" plain icon="Edit" @click="openDialog(true, scope.row)">编辑</el-button>
            <el-popconfirm title="确定删除该用户吗？" @confirm="handleDelete(scope.row)">
              <template #reference>
                <el-button size="small" type="danger" plain icon="Delete">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑用户' : '添加用户'" width="500px">
      <el-form :model="form" label-width="80px" :rules="rules" ref="formRef">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="不修改请留空" v-if="isEdit"/>
          <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" v-else/>
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="form.role" style="width: 100%">
            <el-option label="普通用户" value="user" />
            <el-option label="管理员" value="admin" />
          </el-select>
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

const form = reactive({
  id: null,
  username: '',
  password: '',
  role: 'user'
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
  password: [{ required: false, message: '请输入密码', trigger: 'blur' }] // Dynamic check manually if needed
}

const filteredData = computed(() => {
  if (!searchQuery.value) return tableData.value
  return tableData.value.filter(item => 
    item.username.toLowerCase().includes(searchQuery.value.toLowerCase())
  )
})

const getRoleLabel = (role) => {
  return role === 'admin' ? '管理员' : '普通用户'
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await api.get('/users')
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
    // Map backend roles to form role selection
    if (row.roles && row.roles.length > 0) {
      const roleCode = row.roles[0].code
      form.role = roleCode === 'ROLE_ADMIN' ? 'admin' : 'user'
    } else {
      form.role = 'user'
    }
    form.password = '' // Don't show password
  } else {
    Object.assign(form, { id: null, username: '', password: '', role: 'user' })
  }
}

const handleDelete = async (row) => {
  await api.delete(`/users/${row.id}`)
  ElMessage.success('删除成功')
  fetchData()
}

const save = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      if (!isEdit.value && !form.password) {
        ElMessage.warning('请输入密码')
        return
      }
      
      submitting.value = true
      try {
        if (isEdit.value) {
          await api.put(`/users/${form.id}`, form)
        } else {
          await api.post('/users', form)
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
