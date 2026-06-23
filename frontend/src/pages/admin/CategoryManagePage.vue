<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Plus, Edit, Delete, Refresh } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { categoryApi } from '@/api/modules/category'
import type { Category } from '@/api/types'
import LoadingSkeleton from '@/components/common/LoadingSkeleton.vue'
import EmptyState from '@/components/common/EmptyState.vue'

// ---- State ----
const loading = ref(true)
const error = ref(false)
const categories = ref<Category[]>([])

// ---- Dialog ----
const dialogVisible = ref(false)
const dialogLoading = ref(false)
const isEdit = ref(false)
const editingId = ref<string | null>(null)
const formRef = ref<FormInstance>()

interface CategoryForm {
  name: string
  description: string
  sortOrder: number
}

const form = reactive<CategoryForm>({
  name: '',
  description: '',
  sortOrder: 0,
})

const rules: FormRules = {
  name: [
    { required: true, message: '请输入版块名称', trigger: 'blur' },
    { max: 50, message: '版块名称不能超过50字', trigger: 'blur' },
  ],
  description: [
    { max: 255, message: '描述不能超过255字', trigger: 'blur' },
  ],
}

// ---- Toggle loading ----
const loadingIds = ref(new Set<string>())

// ---- Fetch ----
async function fetchCategories() {
  loading.value = true
  error.value = false
  try {
    const res = await categoryApi.adminGetList()
    categories.value = res.data ?? []
  } catch {
    error.value = true
  } finally {
    loading.value = false
  }
}

// ---- Dialog actions ----
function openCreateDialog() {
  isEdit.value = false
  editingId.value = null
  form.name = ''
  form.description = ''
  form.sortOrder = 0
  formRef.value?.resetFields()
  dialogVisible.value = true
}

function openEditDialog(row: Category) {
  isEdit.value = true
  editingId.value = row.id
  form.name = row.name
  form.description = row.description ?? ''
  form.sortOrder = row.sortOrder
  dialogVisible.value = true
}

async function handleDialogConfirm() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  dialogLoading.value = true
  try {
    if (isEdit.value && editingId.value != null) {
      await categoryApi.update(editingId.value, {
        name: form.name,
        description: form.description || undefined,
        sortOrder: form.sortOrder,
      })
      ElMessage.success('版块已更新')
    } else {
      await categoryApi.create({
        name: form.name,
        description: form.description || undefined,
        sortOrder: form.sortOrder,
      })
      ElMessage.success('版块已创建')
    }
    dialogVisible.value = false
    await fetchCategories()
  } catch (e: unknown) {
    const message = e instanceof Error ? e.message : '操作失败'
    ElMessage.error(message)
  } finally {
    dialogLoading.value = false
  }
}

// ---- Status toggle ----
async function handleToggleStatus(newValue: boolean, row: Category): Promise<boolean> {
  const newStatus = newValue ? 1 : 0
  loadingIds.value.add(row.id)
  try {
    await categoryApi.toggleStatus(row.id, newStatus)
    row.status = newStatus
    ElMessage.success(newStatus === 1 ? '已启用' : '已禁用')
    return true
  } catch (e: unknown) {
    const message = e instanceof Error ? e.message : '操作失败'
    ElMessage.error(message)
    return false
  } finally {
    loadingIds.value.delete(row.id)
  }
}

// ---- Delete ----
async function handleDelete(row: Category) {
  try {
    await ElMessageBox.confirm(
      `确定要删除版块「${row.name}」吗？删除后无法恢复。`,
      '删除版块',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )
    await categoryApi.delete(row.id)
    ElMessage.success('版块已删除')
    await fetchCategories()
  } catch {
    // User cancelled or API error — handled silently
  }
}

// ---- Lifecycle ----
onMounted(() => {
  fetchCategories()
})
</script>

<template>
  <div class="category-manage">
    <!-- Header -->
    <div class="category-manage__header">
      <h2 class="category-manage__title">版块管理</h2>
      <el-button type="primary" :icon="Plus" @click="openCreateDialog">
        新建版块
      </el-button>
    </div>

    <!-- Loading -->
    <LoadingSkeleton v-if="loading" variant="table" />

    <!-- Error -->
    <div v-else-if="error" class="category-manage__error">
      <el-icon :size="48" class="category-manage__error-icon">
        <Refresh />
      </el-icon>
      <p class="category-manage__error-text">加载失败，请稍后重试</p>
      <el-button @click="fetchCategories">重新加载</el-button>
    </div>

    <!-- Empty -->
    <EmptyState
      v-else-if="categories.length === 0"
      title="暂无版块"
      description="点击上方「新建版块」按钮创建第一个版块"
    />

    <!-- Table -->
    <el-table
      v-else
      :data="categories"
      class="category-manage__table"
      stripe
      :default-sort="{ prop: 'sortOrder', order: 'ascending' }"
    >
      <el-table-column prop="name" label="版块名称" min-width="120" />
      <el-table-column label="描述" min-width="200" show-overflow-tooltip>
        <template #default="{ row }">
          {{ row.description || '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="sortOrder" label="排序" width="80" align="center" sortable />
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-switch
            :model-value="row.status === 1"
            :loading="loadingIds.has(row.id)"
            :before-change="(val: boolean) => handleToggleStatus(val, row)"
            size="small"
          />
        </template>
      </el-table-column>
      <el-table-column prop="postCount" label="帖子数" width="80" align="center" />
      <el-table-column label="创建时间" width="160" align="center">
        <template #default="{ row }">
          {{ new Date(row.createTime).toLocaleDateString('zh-CN') }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="140" align="center" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" :icon="Edit" size="small" @click="openEditDialog(row)">
            编辑
          </el-button>
          <el-button link type="danger" :icon="Delete" size="small" @click="handleDelete(row)">
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- Create / Edit Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑版块' : '新建版块'"
      width="480px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="72px">
        <el-form-item label="名称" prop="name">
          <el-input
            v-model="form.name"
            maxlength="50"
            show-word-limit
            placeholder="请输入版块名称"
          />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            maxlength="255"
            show-word-limit
            :rows="3"
            placeholder="请输入版块描述"
          />
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number
            v-model="form.sortOrder"
            :min="0"
            :max="9999"
            controls-position="right"
            placeholder="数字越小越靠前"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="dialogLoading" @click="handleDialogConfirm">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style lang="scss" scoped>
.category-manage {
  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: var(--th-spacing-5);
  }

  &__title {
    font-size: 18px;
    font-weight: 600;
    color: var(--el-text-color-primary);
    margin: 0;
  }

  &__error {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: var(--th-spacing-16) var(--th-spacing-4);
    text-align: center;
  }

  &__error-icon {
    color: var(--el-text-color-placeholder);
  }

  &__error-text {
    margin-top: var(--th-spacing-3);
    margin-bottom: var(--th-spacing-4);
    font-size: 14px;
    color: var(--el-text-color-secondary);
  }

  &__table {
    width: 100%;
  }
}
</style>
