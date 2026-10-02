<script setup lang="ts">
// 社区根入口始终落到全站帖子流，避免用户先看到没有内容的中间页。
await navigateTo('/community/all', {
  replace: true,
  redirectCode: 302,
})
</script>
