<template>
  <span class="player-avatar" :class="{ framed: !owl }">
    <span class="avatar-clip">
      <img
        :src="displaySrc"
        :alt="alt"
        :style="imgStyle"
        draggable="false"
        @error="onError"
      />
    </span>
  </span>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import defaultAvatar from '@/assets/default-avatar.webp'
import { avatarCropScale, isDefaultOwl, resolveAvatarSrc } from '@/utils/avatar'

const props = withDefaults(defineProps<{
  src?: string | null
  alt?: string
}>(), {
  src: '',
  alt: '头像',
})

const broken = ref(false)
watch(() => props.src, () => { broken.value = false })

const owl = computed(() => broken.value || isDefaultOwl(props.src))
const displaySrc = computed(() => broken.value ? defaultAvatar : resolveAvatarSrc(props.src))

const imgStyle = computed(() => {
  if (owl.value) {
    // 只放大夜鸮，对齐好友列表里其它头像的视觉直径
    return { objectFit: 'cover' as const, transform: 'scale(1.16)' }
  }
  const scale = avatarCropScale(props.src)
  const position = props.src?.includes('/images/customer/') ? 'center 18%' : 'center'
  return {
    objectFit: 'cover' as const,
    objectPosition: position,
    transform: `scale(${scale.toFixed(3)})`,
  }
})

function onError() {
  broken.value = true
}
</script>

<style scoped>
.player-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;
  overflow: visible;
  border-radius: 0;
  flex-shrink: 0;
  line-height: 0;
  vertical-align: middle;
}
.avatar-clip {
  display: block;
  overflow: hidden;
  border-radius: 50%;
  width: 100%;
  height: 100%;
}
.player-avatar.framed .avatar-clip {
  width: 66.6%;
  height: 66.6%;
}
img {
  display: block;
  width: 100%;
  height: 100%;
  transform-origin: center center;
}
</style>
