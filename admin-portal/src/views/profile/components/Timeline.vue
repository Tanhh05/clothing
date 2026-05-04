<template>
  <div class="block">
    <el-alert
      v-if="errorMessage"
      :title="errorMessage"
      type="warning"
      show-icon
      :closable="false"
      style="margin-bottom: 12px;"
    />
    <el-timeline>
      <el-timeline-item v-for="(item,index) of timeline" :key="index" :timestamp="item.timestamp" placement="top">
        <el-card>
          <h4>{{ item.title }}</h4>
          <p>{{ item.content }}</p>
        </el-card>
      </el-timeline-item>
    </el-timeline>
  </div>
</template>

<script>
export default {
  data() {
    return {
      timeline: [],
      errorMessage: ''
    }
  },
  computed: {
    resolvedGithubUsername() {
      return String(process.env.VUE_APP_GITHUB_USERNAME || 'Tanhh05').trim()
    }
  },
  watch: {
    resolvedGithubUsername: {
      immediate: true,
      handler() {
        this.loadGithubTimeline()
      }
    }
  },
  methods: {
    async loadGithubTimeline() {
      this.errorMessage = ''
      this.timeline = []
      if (!this.resolvedGithubUsername) {
        this.errorMessage = 'Chưa cấu hình tên người dùng GitHub'
        return
      }
      try {
        const requestUrl = `https://api.github.com/users/${encodeURIComponent(this.resolvedGithubUsername)}/events/public?per_page=10`
        const response = await fetch(requestUrl)
        if (!response.ok) {
          throw new Error(`Lỗi GitHub API ${response.status}`)
        }
        const events = await response.json()
        const mapped = (Array.isArray(events) ? events : []).map(item => {
          const repoName = item && item.repo && item.repo.name ? item.repo.name : 'không-xác-định'
          const eventType = String(item && item.type ? item.type : 'Hoạt động')
          const createdAt = item && item.created_at ? item.created_at : null
          return {
            timestamp: createdAt ? new Date(createdAt).toLocaleString('vi-VN') : 'Không có',
            title: eventType.replace(/Event$/, ''),
            content: `${this.resolvedGithubUsername} • ${repoName}`
          }
        })
        this.timeline = mapped
        if (!this.timeline.length) {
          this.errorMessage = 'Không có hoạt động GitHub công khai gần đây'
        }
      } catch (error) {
        this.errorMessage = 'Không tải được timeline GitHub'
      }
    }
  }
}
</script>
