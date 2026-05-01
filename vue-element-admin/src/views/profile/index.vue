<template>
  <div class="app-container">
    <div v-if="user">
      <el-row :gutter="20">

        <el-col :span="6" :xs="24">
          <user-card :user="user" />
        </el-col>

        <el-col :span="18" :xs="24">
          <el-card>
            <el-tabs v-model="activeTab">
              <el-tab-pane label="Hoạt động" name="activity">
                <activity />
              </el-tab-pane>
              <el-tab-pane label="Dòng thời gian" name="timeline">
                <timeline />
              </el-tab-pane>
              <el-tab-pane label="Tài khoản" name="account">
                <account :user="user" @updated="handleProfileUpdated" />
              </el-tab-pane>
            </el-tabs>
          </el-card>
        </el-col>

      </el-row>
    </div>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import UserCard from './components/UserCard'
import Activity from './components/Activity'
import Timeline from './components/Timeline'
import Account from './components/Account'
import { getMyProfile } from '@/api/user'

const DEFAULT_AVATAR = 'https://wpimg.wallstcn.com/f778738c-e4f8-4870-b634-56703b4acafe.gif'

export default {
  name: 'Profile',
  components: { UserCard, Activity, Timeline, Account },
  data() {
    return {
      user: {},
      activeTab: 'account'
    }
  },
  computed: {
    ...mapGetters([
      'name',
      'avatar',
      'roles'
    ])
  },
  created() {
    this.getUser()
  },
  methods: {
    async getUser() {
      try {
        const data = await getMyProfile()
        const roleText = Array.isArray(data && data.roles) ? data.roles.join(' | ') : this.roles.join(' | ')
        this.user = {
          id: data && data.id,
          username: data && data.username,
          name: (data && data.fullName) || (data && data.username) || this.name,
          fullName: (data && data.fullName) || '',
          email: (data && data.email) || '',
          phone: (data && data.phone) || '',
          status: (data && data.status) || '',
          role: roleText || 'USER',
          avatar: this.avatar || DEFAULT_AVATAR
        }
      } catch (error) {
        this.user = {
          name: this.name,
          fullName: this.name,
          role: this.roles.join(' | '),
          email: '',
          phone: '',
          avatar: this.avatar || DEFAULT_AVATAR
        }
      }
    },
    async handleProfileUpdated() {
      await this.getUser()
    }
  }
}
</script>
