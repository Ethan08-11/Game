<template>
  <div class="friend-panel">
    <div class="friend-list-container">
      <div class="friend-scroll-area">
        <div v-if="friends.length === 0" class="empty-tip">
          暂无好友
        </div>
        <div v-for="f in friends" :key="f.id" class="friend-row">
          <img :src="f.avatarUrl || defaultAvatar" class="friend-avatar" />
          <span class="name">{{ f.displayName || f.username }}</span>
          <img :src="getStatusIcon(f)" class="status-icon" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useUserStore, type Friend } from '@/store/user'
import defaultAvatar from '@/assets/default-avatar.jpeg'
import statusOnlineIcon from '@/assets/status-online.png'
import statusOfflineIcon from '@/assets/status-offline.png'
import statusInGameIcon from '@/assets/status-in-game.jpeg'
import statusInTeamIcon from '@/assets/status-in-team.jpeg'

const userStore = useUserStore()

const friends = computed(() => userStore.friends)

function getStatusIcon(f: Friend): string {
  switch (f.presenceStatus) {
    case 'IN_MATCH':
      return statusInGameIcon
    case 'IN_ROOM':
      return statusInTeamIcon
    case 'OFFLINE':
      return statusOfflineIcon
    default:
      return statusOnlineIcon
  }
}
</script>

<style scoped>
.friend-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  margin-top: 0;
}

.friend-list-container {
  flex: 1;
  min-height: 0;
  margin-top: 60px;
  border: none;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.friend-scroll-area {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 0;
  padding-top: 0;
  padding-bottom: var(--space-4);
}

.empty-tip {
  color: var(--color-text-tertiary);
  font-size: var(--text-sm);
  padding: var(--space-2);
  text-align: center;
}

.friend-row {
  display: flex;
  align-items: center;
  height: 320px;
  flex-shrink: 0;
  padding: 12px var(--space-5) 0 var(--space-3);
  background: url('@/assets/friend-row-bg.jpeg') center/100% 100% no-repeat;
  position: relative;
  margin-top: -260px;

}
.friend-row:first-child {
  margin-top: -130px;
}

.friend-avatar {
  width: 72px;
  height: 72px;
  border-radius: var(--radius-full);
  object-fit: cover;
  flex-shrink: 0;
  position: relative;
  z-index: 1;
}

.name {
  flex: 1;
  color: #4a3520;
  font-size: calc(var(--text-base) * 1.5);
  margin-left: calc(var(--space-3) - 1em);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  position: relative;
  z-index: 1;
}

.status-icon {
  height: 54px;
  object-fit: contain;
  flex-shrink: 0;
  position: relative;
  z-index: 1;
}
</style>
