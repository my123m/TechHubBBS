<script setup lang="ts">
import { reactive, ref, watch, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import MdEditor from '@/components/markdown/MdEditor.vue'
import VisibilitySelector from '@/components/post/VisibilitySelector.vue'
import { postApi } from '@/api/modules/post'
import { categoryApi } from '@/api/modules/category'
import { useDraft } from '@/composables/useDraft'
import type { Category, PostVisibility } from '@/api/types'

const router = useRouter()

// ---- Form state ----
const formRef = ref<FormInstance>()
const submitting = ref(false)
const categoriesLoading = ref(true)
const categories = ref<Category[]>([])

const form = reactive({
  title: '',
  content: '',
  categoryId: undefined as string | undefined,
  visibility: 0 as PostVisibility,
})

// ---- Validation ----
const rules: FormRules = {
  title: [
    { required: true, message: '请输入帖子标题', trigger: 'blur' },
    { max: 200, message: '标题不能超过200字', trigger: 'blur' },
  ],
  content: [
    { required: true, message: '请输入帖子内容', trigger: 'blur' },
  ],
  categoryId: [
    { required: true, message: '请选择版块', trigger: 'change' },
  ],
}

// ---- Draft auto-save ----
const {
  currentDraftId,
  draftData,
  isDirty,
  checkDraft,
  restoreDraft,
  discardDraft,
  startAutoSave,
  stopAutoSave,
} = useDraft(undefined, submitting)

// Sync form values → draftData so auto-save picks up changes
watch(
  () => ({ title: form.title, content: form.content, categoryId: form.categoryId, visibility: form.visibility }),
  (data) => {
    draftData.value = { ...draftData.value, ...data }
  },
  { deep: true },
)

// ---- Data loading ----
async function loadCategories() {
  try {
    const res = await categoryApi.getList()
    categories.value = res.data || []
  } catch {
    ElMessage.error('加载版块列表失败')
  } finally {
    categoriesLoading.value = false
  }
}

// ---- Draft check & restore ----
async function checkExistingDraft() {
  const draft = await checkDraft()
  if (!draft?.id) return

  try {
    await ElMessageBox.confirm('检测到未保存的草稿，是否恢复？', '草稿恢复', {
      confirmButtonText: '恢复',
      cancelButtonText: '放弃',
      type: 'info',
    })
    const restored = await restoreDraft(draft.id)
    if (restored) {
      form.title = restored.title || ''
      form.content = restored.content || ''
      form.categoryId = restored.categoryId
      form.visibility = (restored.visibility as PostVisibility) ?? 0
    }
  } catch {
    // User chose to discard
    await discardDraft(draft.id)
  }
}

// ---- Submit ----
async function handleSubmit() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  if (!form.title.trim() || !form.content.trim() || form.categoryId == null) {
    ElMessage.warning('请完善帖子信息')
    return
  }

  submitting.value = true
  try {
    const res = await postApi.create({
      title: form.title,
      content: form.content,
      categoryId: form.categoryId,
      visibility: form.visibility,
      draftPostId: currentDraftId.value || undefined,
    })
    // Clean up draft after successful publish
    if (currentDraftId.value) {
      await discardDraft()
    }
    // Prevent stopAutoSave() from re-creating a draft during unmount
    isDirty.value = false
    draftData.value = {}
    ElMessage.success('发布成功')
    router.push(`/posts/${res.data.id}`)
  } catch (e: unknown) {
    const message = e instanceof Error ? e.message : '发布失败，草稿已保存'
    ElMessage.error(message)
  } finally {
    submitting.value = false
  }
}

// ---- Lifecycle ----
onMounted(async () => {
  await loadCategories()
  await checkExistingDraft()
  startAutoSave()
})

onUnmounted(() => {
  stopAutoSave()
})
</script>

<template>
  <div class="post-form-page">
    <header class="post-form-page__header">
      <h1 class="post-form-page__title">发布帖子</h1>
      <p class="post-form-page__subtitle">分享你的技术见解</p>
    </header>

    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-position="top"
      hide-required-asterisk
      class="post-form-page__form"
      @submit.prevent="handleSubmit"
    >
      <el-form-item label="标题" prop="title">
        <el-input
          v-model="form.title"
          placeholder="请输入帖子标题"
          maxlength="200"
          show-word-limit
        />
      </el-form-item>

      <el-form-item label="版块" prop="categoryId">
        <el-select
          v-model="form.categoryId"
          placeholder="请选择版块"
          :loading="categoriesLoading"
          class="post-form-page__category-select"
        >
          <el-option
            v-for="cat in categories"
            :key="cat.id"
            :label="cat.name"
            :value="cat.id"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="可见范围">
        <VisibilitySelector v-model="form.visibility" />
      </el-form-item>

      <el-form-item label="内容" prop="content">
        <MdEditor v-model="form.content" />
      </el-form-item>

      <el-form-item>
        <el-button
          type="primary"
          native-type="submit"
          :loading="submitting"
          class="post-form-page__submit"
        >
          {{ submitting ? '发布中...' : '发布帖子' }}
        </el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<style lang="scss" scoped>
.post-form-page {
  max-width: var(--th-content-max-width, 800px);
  margin: 0 auto;
  padding: var(--th-spacing-8, 32px) var(--th-spacing-4, 16px);
}

.post-form-page__header {
  margin-bottom: var(--th-spacing-8, 32px);
}

.post-form-page__title {
  font-size: 24px;
  font-weight: 700;
  color: var(--el-text-color-primary);
  margin: 0 0 var(--th-spacing-1, 4px);
}

.post-form-page__subtitle {
  font-size: 14px;
  color: var(--el-text-color-secondary);
  margin: 0;
}

.post-form-page__form {
  :deep(.el-form-item__label) {
    font-weight: 600;
    color: var(--el-text-color-primary);
  }
}

.post-form-page__category-select {
  width: 100%;
  max-width: 320px;
}

.post-form-page__submit {
  min-width: 140px;
  font-weight: 600;
  letter-spacing: 1px;
}
</style>
