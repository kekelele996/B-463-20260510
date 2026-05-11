<template>
  <div class="dashboard-container">
    <el-row :gutter="20">
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <template #header>
            <div class="card-header">
              <span>总资产数量</span>
              <el-tag type="success">实时</el-tag>
            </div>
          </template>
          <div class="stat-value">{{ totalAssets }}</div>
          <div class="stat-desc">当前系统录入的所有设备与车辆</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <template #header>
            <div class="card-header">
              <span>在线设备</span>
              <el-tag>在线</el-tag>
            </div>
          </template>
          <div class="stat-value">{{ onlineEquipments }}</div>
          <div class="stat-desc">运行状态正常的设备数量</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <template #header>
            <div class="card-header">
              <span>在途车辆</span>
              <el-tag type="warning">行驶中</el-tag>
            </div>
          </template>
          <div class="stat-value">{{ activeVehicles }}</div>
          <div class="stat-desc">当前正在执行任务的车辆</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>设备状态分布</span>
            </div>
          </template>
          <div ref="equipmentChart" style="height: 300px"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>车辆里程统计 (Top 5)</span>
            </div>
          </template>
          <div ref="vehicleChart" style="height: 300px"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import * as echarts from 'echarts'
import api from '../api'

const totalAssets = ref(0)
const onlineEquipments = ref(0)
const activeVehicles = ref(0)
const equipmentChart = ref(null)
const vehicleChart = ref(null)

const initCharts = (equipments, vehicles) => {
  // Equipment Pie Chart
  const eChart = echarts.init(equipmentChart.value)
  const statusCount = {}
  equipments.forEach(e => {
    statusCount[e.status] = (statusCount[e.status] || 0) + 1
  })
  const statusMap = {
    'online': '在线',
    'offline': '离线',
    'maintenance': '维护中',
    'driving': '行驶中',
    'parked': '停泊中'
  }
  const pieData = Object.keys(statusCount).map(k => ({ value: statusCount[k], name: statusMap[k] || k }))
  
  eChart.setOption({
    tooltip: { trigger: 'item' },
    legend: { top: '5%', left: 'center' },
    series: [
      {
        name: 'Access From',
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 10,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: { show: false, position: 'center' },
        emphasis: {
          label: { show: true, fontSize: 20, fontWeight: 'bold' }
        },
        labelLine: { show: false },
        data: pieData
      }
    ]
  })

  // Vehicle Bar Chart
  const vChart = echarts.init(vehicleChart.value)
  const sortedVehicles = [...vehicles].sort((a, b) => b.mileage - a.mileage).slice(0, 5)
  
  vChart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: [
      {
        type: 'category',
        data: sortedVehicles.map(v => v.licensePlate),
        axisTick: { alignWithLabel: true }
      }
    ],
    yAxis: [{ type: 'value' }],
    series: [
      {
        name: 'Mileage',
        type: 'bar',
        barWidth: '60%',
        data: sortedVehicles.map(v => v.mileage),
        itemStyle: { color: '#409EFF' }
      }
    ]
  })

  // Resize charts on window resize
  window.addEventListener('resize', () => {
    eChart.resize()
    vChart.resize()
  })
}

const fetchData = async () => {
  try {
    const [equipRes, vehicleRes] = await Promise.all([
      api.get('/equipments'),
      api.get('/vehicles')
    ])
    
    if (equipRes.data.code === 200 && vehicleRes.data.code === 200) {
      const equipments = equipRes.data.data
      const vehicles = vehicleRes.data.data
      
      totalAssets.value = equipments.length + vehicles.length
      onlineEquipments.value = equipments.filter(e => e.status === 'online').length
      activeVehicles.value = vehicles.filter(v => v.status === 'driving').length
      
      initCharts(equipments, vehicles)
    }
  } catch (error) {
    console.error('Failed to fetch dashboard data', error)
  }
}

onMounted(fetchData)
</script>

<style scoped>
.stat-card {
  height: 100%;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.stat-value {
  font-size: 32px;
  font-weight: bold;
  color: #303133;
  margin: 10px 0;
}
.stat-desc {
  font-size: 14px;
  color: #909399;
}
</style>
