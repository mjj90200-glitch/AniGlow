<template>
  <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pt-20 lg:pt-24 pb-4 lg:pb-24">
    <AgentMobileChat :chat="chat" />
    <AgentGalgameLayout :chat="chat" />
    <AgentMembershipModal :chat="chat" />
  </div>
</template>

<script setup lang="ts">
const chat = reactive(await useAgentChatPage())
</script>
