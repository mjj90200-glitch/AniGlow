<template>
  <div class="min-h-screen relative overflow-hidden">

    <!-- ═══════════════════════════════════════════════════════════
         背景光斑 · Firefly Glow Spots
         ════════════════════════════════════════════════════════ -->

    <!-- 左上 — 樱花粉光斑 -->
    <div
      class="glow-spot glow-spot-sakura w-96 h-96 -top-32 -left-24"
      style="animation-delay: 0s"
    />

    <!-- 右上 — 淡蓝光斑 -->
    <div
      class="glow-spot glow-spot-sky w-80 h-80 top-40 -right-20"
      style="animation-delay: 2s"
    />

    <!-- 中部 — 萤火绿微光 -->
    <div
      class="glow-spot glow-spot-firefly w-64 h-64 top-1/3 left-1/3"
      style="animation-delay: 4s"
    />

    <!-- 右下 — 薰衣草紫光斑 -->
    <div
      class="absolute rounded-full pointer-events-none -bottom-20 -right-16 w-96 h-96"
      style="
        background: rgba(197, 185, 232, 0.08);
        filter: blur(70px);
        animation: glowSpotFloat 11s ease-in-out infinite;
        animation-delay: 1s;
        z-index: 0;
      "
    />

    <!-- ═══════════════════════════════════════════════════════════
         导航栏 · Glass Navigation
         ═══════════════════════════════════════════════════════════ -->

    <nav
      class="fixed top-3 left-1/2 -translate-x-1/2 z-50 w-[95%] max-w-7xl
             transition-all duration-500"
      :class="[
        scrolled
          ? 'glass-nav scrolled rounded-4xl'
          : 'bg-transparent rounded-4xl'
      ]"
    >
      <div class="flex items-center justify-between px-6 py-3.5">

        <!-- Logo -->
        <NuxtLink
          to="/"
          class="flex items-center gap-2.5 group shrink-0"
        >
          <div
            class="w-10 h-10 rounded-2xl flex items-center justify-center
                   transition-all duration-300 group-hover:shadow-glow group-hover:scale-105"
            style="background: linear-gradient(135deg, #00E676 0%, #00C853 100%);"
          >
            <Sparkles class="w-5 h-5 text-white" />
          </div>
          <div class="flex flex-col leading-none">
            <span class="text-lg font-extrabold tracking-tight"
                  style="background: linear-gradient(135deg, #4098C3 0%, #00C853 50%, #FF9AAE 100%); -webkit-background-clip: text; -webkit-text-fill-color: transparent;">
              萤火番舍
            </span>
            <span class="text-[10px] text-gray-400 font-medium tracking-widest uppercase">AniGlow</span>
          </div>
        </NuxtLink>

        <!-- 桌面端导航链接 -->
        <div class="hidden lg:flex items-center gap-1">
          <NuxtLink
            v-for="item in navItems"
            :key="item.path"
            :to="item.path"
            class="relative px-4 py-2 rounded-full text-sm font-semibold
                   transition-all duration-300"
            :class="[
              route.path === item.path
                ? 'text-gray-800'
                : 'text-gray-500 hover:text-gray-700'
            ]"
          >
            <!-- 当前页指示条 -->
            <span
              v-if="route.path === item.path"
              class="absolute inset-x-3 -bottom-0.5 h-0.5 rounded-full"
              style="background: linear-gradient(90deg, #00E676, #A0D8EF);"
            />
            {{ item.name }}
          </NuxtLink>
        </div>

        <!-- 右侧操作区 -->
        <div class="flex items-center gap-2.5 shrink-0">
          <!-- 搜索 -->
          <button
            class="w-10 h-10 flex items-center justify-center rounded-full
                   transition-all duration-300 hover:scale-105"
            :class="[
              scrolled
                ? 'bg-white/60 backdrop-blur-md hover:bg-white/90 shadow-soft'
                : 'bg-white/50 backdrop-blur-sm hover:bg-white/80'
            ]"
            aria-label="搜索"
            aria-haspopup="dialog"
            @click="openGlobalSearch"
          >
            <Search class="w-4.5 h-4.5 text-gray-500" />
          </button>

          <!-- ═══ 未登录：注册/登录按钮 ═══ -->
          <button
            v-if="!userStore.isLoggedIn"
            class="flex items-center gap-1.5 sm:gap-2 px-3.5 py-2 sm:px-5 sm:py-2.5 rounded-full
                   text-xs sm:text-sm font-bold text-white
                   transition-all duration-300 hover:scale-105 active:scale-95"
            style="
              background: linear-gradient(135deg, #00E676 0%, #00C853 100%);
              box-shadow: 0 4px 16px rgba(0, 230, 118, 0.3);
            "
            @mouseenter="(e: MouseEvent) => {
              (e.target as HTMLElement).style.boxShadow = '0 8px 28px rgba(0, 230, 118, 0.45)'
            }"
            @mouseleave="(e: MouseEvent) => {
              (e.target as HTMLElement).style.boxShadow = '0 4px 16px rgba(0, 230, 118, 0.3)'
            }"
            @click="openAuthingGuard"
          >
            <UserPlus class="w-3.5 h-3.5 sm:w-4 sm:h-4" />
            <span class="hidden sm:inline">加入番舍</span>
            <span class="sm:hidden">登录</span>
          </button>

          <!-- ═══ 已登录：用户头像 + 下拉菜单 ═══ -->
          <div
            v-else
            class="flex relative"
            @mouseenter="userMenuOpen = true"
            @mouseleave="userMenuOpen = false"
          >
            <!-- 用户按钮 -->
            <button
              class="flex items-center gap-2.5 px-2 py-1.5 sm:px-3 sm:py-1.5 rounded-full
                     transition-all duration-300 hover:shadow-soft"
              :class="[
                scrolled
                  ? 'bg-white/60 backdrop-blur-md hover:bg-white/85'
                  : 'bg-white/40 backdrop-blur-sm hover:bg-white/70'
              ]"
              @click.stop="userMenuOpen = !userMenuOpen"
            >
              <!-- 头像 -->
              <div class="w-8 h-8 rounded-full overflow-hidden ring-2 ring-white/60 shrink-0">
                <img
                  v-if="userStore.user?.avatar"
                  :src="userStore.user.avatar"
                  :alt="userStore.user.name"
                  class="w-full h-full object-cover"
                  @error="(e: Event) => { (e.target as HTMLImageElement).style.display = 'none' }"
                />
                <div
                  v-else
                  class="w-full h-full flex items-center justify-center text-sm font-bold text-white"
                  style="background: linear-gradient(135deg, #A0D8EF, #7BC4E0);"
                >
                  {{ (userStore.user?.name || '?')[0] }}
                </div>
              </div>
              <!-- 用户名（仅桌面端） -->
              <span class="hidden sm:inline text-sm font-semibold text-gray-700 max-w-[80px] truncate">
                {{ userStore.user?.name || '番舍同好' }}
              </span>
              <ChevronDown
                class="hidden sm:block w-3.5 h-3.5 text-gray-400 transition-transform duration-300"
                :class="{ 'rotate-180': userMenuOpen }"
              />
            </button>

            <!-- 下拉菜单 -->
            <Transition
              enter-active-class="transition-all duration-200 ease-out"
              enter-from-class="opacity-0 -translate-y-1 scale-95"
              enter-to-class="opacity-100 translate-y-0 scale-100"
              leave-active-class="transition-all duration-150 ease-in"
              leave-from-class="opacity-100 translate-y-0 scale-100"
              leave-to-class="opacity-0 -translate-y-1 scale-95"
            >
              <div
                v-if="userMenuOpen"
                class="absolute right-0 top-full mt-2 w-44 py-2 rounded-2xl
                       bg-white/85 backdrop-blur-2xl shadow-glaze-lg border border-white/50"
              >
                <!-- 用户信息 -->
                <div class="px-4 py-2 border-b border-white/40">
                  <p class="text-sm font-bold text-gray-800 truncate">{{ userStore.user?.name }}</p>
                  <p class="text-xs text-gray-400 truncate">{{ userStore.user?.phone || '' }}</p>
                </div>
                <!-- 菜单项 -->
                <button
                  class="w-full flex items-center gap-2.5 px-4 py-2.5 text-sm text-gray-600
                         hover:bg-cream-200 transition-colors duration-200"
                  @click="showProfileModal = true; userMenuOpen = false"
                >
                  <UserRoundCog class="w-4 h-4 text-gray-400" />
                  编辑资料
                </button>
                <NuxtLink
                  to="/user/posts"
                  class="w-full flex items-center gap-2.5 px-4 py-2.5 text-sm text-gray-600
                         hover:bg-cream-200 transition-colors duration-200"
                  @click="userMenuOpen = false"
                >
                  <FileText class="w-4 h-4 text-gray-400" />
                  我的帖子
                </NuxtLink>
                <button
                  class="w-full flex items-center gap-2.5 px-4 py-2.5 text-sm text-gray-600
                         hover:bg-cream-200 transition-colors duration-200"
                  @click="handleLogout"
                >
                  <LogOut class="w-4 h-4 text-gray-400" />
                  退出登录
                </button>
              </div>
            </Transition>
          </div>

          <!-- 移动端菜单按钮 -->
          <button
            class="lg:hidden w-10 h-10 flex items-center justify-center rounded-full
                   bg-white/60 backdrop-blur-md shadow-soft active:scale-95 transition-transform"
            @click="mobileOpen = !mobileOpen"
            aria-label="菜单"
          >
            <Menu v-if="!mobileOpen" class="w-5 h-5 text-gray-600" />
            <X v-else class="w-5 h-5 text-gray-600" />
          </button>
        </div>
      </div>

      <!-- ═══════════════════════════════════════════════════════
           移动端下拉菜单
           ════════════════════════════════════════════════════ -->

      <Transition
        enter-active-class="transition-all duration-300 ease-out"
        enter-from-class="opacity-0 -translate-y-2 scale-95"
        enter-to-class="opacity-100 translate-y-0 scale-100"
        leave-active-class="transition-all duration-200 ease-in"
        leave-from-class="opacity-100 translate-y-0 scale-100"
        leave-to-class="opacity-0 -translate-y-2 scale-95"
      >
        <div
          v-if="mobileOpen"
          class="lg:hidden mx-2 mb-3 px-4 py-4 rounded-3xl
                 bg-white/85 backdrop-blur-2xl shadow-glaze-lg border border-white/50"
        >
          <div class="flex flex-col gap-1.5">
            <NuxtLink
              v-for="item in navItems"
              :key="item.path"
              :to="item.path"
              class="flex items-center gap-3 px-4 py-3 rounded-2xl text-sm font-semibold
                     transition-all duration-200"
              :class="[
                route.path === item.path
                  ? 'bg-firefly/10 text-firefly-700'
                  : 'text-gray-600 hover:bg-cream-200'
              ]"
              @click="mobileOpen = false"
            >
              <component :is="item.icon" class="w-4 h-4" />
              {{ item.name }}
            </NuxtLink>

            <!-- 分隔 -->
            <hr class="my-1 divider-glow" />

            <!-- 移动端已登录用户信息 -->
            <template v-if="userStore.isLoggedIn">
              <div class="flex items-center gap-3 px-4 py-2">
                <div class="w-10 h-10 rounded-full overflow-hidden ring-2 ring-white/60 shrink-0">
                  <img
                    v-if="userStore.user?.avatar"
                    :src="userStore.user.avatar"
                    :alt="userStore.user.name"
                    class="w-full h-full object-cover"
                    @error="(e: Event) => { (e.target as HTMLImageElement).style.display = 'none' }"
                  />
                  <div
                    v-else
                    class="w-full h-full flex items-center justify-center text-sm font-bold text-white"
                    style="background: linear-gradient(135deg, #A0D8EF, #7BC4E0);"
                  >
                    {{ (userStore.user?.name || '?')[0] }}
                  </div>
                </div>
                <div class="flex-1 min-w-0">
                  <p class="text-sm font-bold text-gray-800 truncate">{{ userStore.user?.name }}</p>
                  <p class="text-xs text-gray-400 truncate">{{ userStore.user?.phone || '' }}</p>
                </div>
              </div>
              <button
                class="flex items-center gap-3 px-4 py-3 rounded-2xl text-sm font-semibold
                       text-gray-600 hover:bg-cream-200 transition-all duration-200 w-full"
                @click="mobileOpen = false; showProfileModal = true"
              >
                <UserRoundCog class="w-4 h-4" />
                编辑资料
              </button>
              <NuxtLink
                to="/user/posts"
                class="flex items-center gap-3 px-4 py-3 rounded-2xl text-sm font-semibold
                       text-gray-600 hover:bg-cream-200 transition-all duration-200 w-full"
                @click="mobileOpen = false"
              >
                <FileText class="w-4 h-4" />
                我的帖子
              </NuxtLink>
              <button
                class="flex items-center gap-3 px-4 py-3 rounded-2xl text-sm font-semibold
                       text-sakura-dark hover:bg-sakura/10 transition-all duration-200 w-full"
                @click="handleLogout"
              >
                <LogOut class="w-4 h-4" />
                退出登录
              </button>
            </template>

            <!-- 移动端未登录 -->
            <button
              v-else
              class="flex items-center justify-center gap-2 w-full py-3 rounded-full
                     text-sm font-bold text-white
                     transition-all duration-300 active:scale-95"
              style="background: linear-gradient(135deg, #00E676 0%, #00C853 100%);"
              @click="openAuthingGuard"
            >
              <UserPlus class="w-4 h-4" />
              加入番舍
            </button>
          </div>
        </div>
      </Transition>
    </nav>

    <Teleport to="body">
      <Transition
        enter-active-class="transition-all duration-300 ease-out"
        enter-from-class="opacity-0"
        enter-to-class="opacity-100"
        leave-active-class="transition-all duration-200 ease-in"
        leave-from-class="opacity-100"
        leave-to-class="opacity-0"
      >
        <div
          v-if="showGlobalSearch"
          class="fixed inset-0 z-[95] flex items-start justify-center px-4 pt-24 sm:pt-32"
          role="dialog"
          aria-modal="true"
          aria-labelledby="global-search-title"
          style="background: rgba(35, 50, 44, 0.18); backdrop-filter: blur(10px);"
          @click.self="closeGlobalSearch"
        >
          <section class="w-full max-w-2xl rounded-[2rem] border border-white/70 bg-white/80 p-5 shadow-[0_28px_80px_rgba(31,68,47,0.18)] backdrop-blur-2xl sm:p-7">
            <div class="mb-5 flex items-start justify-between gap-4">
              <div>
                <p class="text-[11px] font-black uppercase tracking-[0.24em] text-firefly-600">Find Your Story</p>
                <h2 id="global-search-title" class="mt-1 text-xl font-extrabold text-gray-800">搜索喜欢的番剧</h2>
              </div>
              <button
                class="flex h-9 w-9 items-center justify-center rounded-full bg-white/75 text-gray-400 transition hover:bg-white hover:text-gray-700"
                aria-label="关闭搜索"
                @click="closeGlobalSearch"
              >
                <X class="h-4 w-4" />
              </button>
            </div>

            <form class="relative" @submit.prevent="submitGlobalSearch">
              <Search class="pointer-events-none absolute left-4 top-1/2 h-5 w-5 -translate-y-1/2 text-gray-400" />
              <input
                ref="globalSearchInputRef"
                v-model="globalSearchQuery"
                type="search"
                autocomplete="off"
                placeholder="支持中文名、原名或类型关键词"
                class="w-full rounded-2xl border border-white/80 bg-white/80 py-4 pl-12 pr-28 text-sm font-semibold text-gray-700 outline-none transition focus:border-firefly/40 focus:ring-4 focus:ring-firefly/10"
              />
              <button
                type="submit"
                class="absolute right-2 top-1/2 -translate-y-1/2 rounded-full bg-firefly px-5 py-2 text-sm font-extrabold text-white shadow-glow-sm transition hover:bg-firefly-600 disabled:opacity-40"
                :disabled="!globalSearchQuery.trim()"
              >
                去搜索
              </button>
            </form>

            <div class="mt-5 flex flex-wrap items-center gap-2">
              <span class="mr-1 text-xs font-bold text-gray-400">试试</span>
              <button
                v-for="keyword in searchSuggestions"
                :key="keyword"
                type="button"
                class="rounded-full border border-white/80 bg-cream/75 px-3 py-1.5 text-xs font-bold text-gray-500 transition hover:border-firefly/30 hover:text-firefly-700"
                @click="searchByKeyword(keyword)"
              >
                {{ keyword }}
              </button>
            </div>
          </section>
        </div>
      </Transition>
    </Teleport>

    <Transition
      enter-active-class="transition-all duration-300 ease-out"
      enter-from-class="opacity-0"
      enter-to-class="opacity-100"
      leave-active-class="transition-all duration-200 ease-in"
      leave-from-class="opacity-100"
      leave-to-class="opacity-0"
    >
      <div
        v-if="authModalOpen"
        class="fixed inset-0 z-[80] flex items-center justify-center px-4 py-8"
        style="background: rgba(0,0,0,0.18); backdrop-filter: blur(6px);"
        @click.self="closeAuthingGuard"
      >
        <!-- 弹窗光斑装饰 -->
        <div class="absolute pointer-events-none w-64 h-64 rounded-full"
          style="top: 15%; left: 10%; background: rgba(255,192,203,0.06); filter: blur(60px); animation: glowSpotFloat 8s ease-in-out infinite;" />
        <div class="absolute pointer-events-none w-48 h-48 rounded-full"
          style="bottom: 20%; right: 15%; background: rgba(0,230,118,0.05); filter: blur(50px); animation: glowSpotFloat 10s ease-in-out infinite reverse;" />

        <div class="authing-glass-shell relative w-full max-w-[440px] animate-scale-in"
          style="background: linear-gradient(160deg, rgba(253,251,247,0.88) 0%, rgba(250,246,238,0.82) 100%);
                 backdrop-filter: blur(24px);">
          <!-- 顶部装饰条 -->
          <div class="h-1.5 rounded-full mx-6 mt-4"
            style="background: linear-gradient(90deg, #FFC0CB, #A0D8EF, #00E676); opacity: 0.6;" />

          <!-- 标题 -->
          <div class="text-center pt-5 pb-1">
            <div class="w-12 h-12 mx-auto mb-2 rounded-xl flex items-center justify-center"
              style="background: linear-gradient(135deg, #00E676 0%, #00C853 100%); box-shadow: 0 4px 16px rgba(0,230,118,0.25);">
              <Sparkles class="w-6 h-6 text-white" />
            </div>
            <h2 class="text-lg font-extrabold"
              style="background: linear-gradient(135deg, #4098C3 0%, #00C853 50%, #FF9AAE 100%); -webkit-background-clip: text; -webkit-text-fill-color: transparent;">
              加入萤火番舍
            </h2>
            <p class="text-xs text-gray-400 mt-0.5">登录后享受完整的番舍体验</p>
          </div>

          <button
            class="absolute right-4 top-4 z-10 flex h-8 w-8 items-center justify-center rounded-full
                   bg-white/60 text-gray-400 hover:text-gray-600 hover:bg-white/90
                   transition-all hover:scale-110 shadow-sm"
            aria-label="关闭登录"
            @click="closeAuthingGuard"
          >
            <X class="h-3.5 w-3.5" />
          </button>

          <!-- 权限错误提示 -->
          <div
            v-if="loginError"
            class="mx-4 mt-4 p-4 rounded-2xl text-sm animate-slide-up"
            style="background: rgba(255, 192, 203, 0.15); border: 1px solid rgba(255, 154, 174, 0.3); color: #D0707F;"
          >
            <div class="flex items-start gap-2">
              <AlertCircle class="w-4 h-4 mt-0.5 shrink-0" />
              <div>
                <p class="font-semibold mb-1">登录失败</p>
                <p class="text-xs opacity-80">{{ loginError }}</p>
                <p class="text-xs mt-2 opacity-70">
                  请检查用户名和密码，或稍后重试。
                </p>
              </div>
            </div>
          </div>

          <div id="authing-guard-container">
            <LoginForm @login="handleAuthingLogin" />
          </div>
        </div>
      </div>
    </Transition>

    <!-- ═══════════════════════════════════════════════════════════
         新用户资料完善弹窗
         ════════════════════════════════════════════════════════ -->

    <ProfileSetupModal
      v-model:visible="showProfileModal"
      @complete="handleProfileComplete"
    />

    <CredentialsSetupModal
      :visible="userStore.needsCredentials"
      @complete="triggerFireflyBurst"
      @logout="handleLogout"
    />

    <div
      v-if="fireflyBurst"
      class="pointer-events-none fixed inset-x-0 bottom-10 z-[90] mx-auto h-40 w-48"
      aria-hidden="true"
    >
      <span
        v-for="i in 9"
        :key="i"
        class="login-firefly"
        :style="{
          left: `${12 + i * 8}%`,
          animationDelay: `${i * 0.07}s`,
        }"
      />
    </div>

    <!-- ═══════════════════════════════════════════════════════════
         主内容区
         ═══════════════════════════════════════════════════════════ -->

    <main class="relative z-10 pb-28 lg:pb-0">
      <PullToRefresh>
        <slot />
      </PullToRefresh>
    </main>

    <PwaInstallPrompt />
    <MobileBottomNav />

    <!-- ═══════════════════════════════════════════════════════════
         页脚 · Glass Footer
         ═══════════════════════════════════════════════════════════ -->

    <footer class="relative z-10 mt-24 hidden pb-10 lg:block">
      <div class="max-w-7xl mx-auto px-6">
        <!-- 琉璃卡片包裹 -->
        <div
          class="glass-card-cream rounded-4xl px-8 py-8 md:px-12"
        >
          <div class="flex flex-col md:flex-row items-center justify-between gap-6">
            <!-- 品牌 -->
            <div class="flex items-center gap-3">
              <div
                class="w-9 h-9 rounded-xl flex items-center justify-center"
                style="background: linear-gradient(135deg, #00E676 0%, #00C853 100%);"
              >
                <Sparkles class="w-4.5 h-4.5 text-white" />
              </div>
              <div>
                <span class="text-sm font-extrabold tracking-tight"
                      style="background: linear-gradient(135deg, #4098C3 0%, #00C853 50%, #FF9AAE 100%); -webkit-background-clip: text; -webkit-text-fill-color: transparent;">
                  萤火番舍
                </span>
                <span class="ml-2 text-[10px] text-gray-400 tracking-widest uppercase">AniGlow</span>
              </div>
            </div>

            <!-- 版权 -->
            <p class="text-xs text-gray-400 flex items-center gap-1.5">
              <Heart class="w-3.5 h-3.5 text-sakura fill-sakura" />
              Made with love for anime lovers &copy; 2026
            </p>

            <!-- 社交 -->
            <div class="flex items-center gap-3">
              <a
                href="#"
                class="w-9 h-9 flex items-center justify-center rounded-full
                       bg-white/60 backdrop-blur-sm border border-white/40
                       hover:shadow-glaze transition-all duration-300 hover:scale-110"
                aria-label="GitHub"
              >
                <Github class="w-4 h-4 text-gray-500" />
              </a>
              <a
                href="#"
                class="w-9 h-9 flex items-center justify-center rounded-full
                       bg-white/60 backdrop-blur-sm border border-white/40
                       hover:shadow-glaze transition-all duration-300 hover:scale-110"
                aria-label="Twitter"
              >
                <Twitter class="w-4 h-4 text-gray-500" />
              </a>
              <a
                href="#"
                class="w-9 h-9 flex items-center justify-center rounded-full
                       bg-white/60 backdrop-blur-sm border border-white/40
                       hover:shadow-glaze transition-all duration-300 hover:scale-110"
                aria-label="Discord"
              >
                <MessageCircle class="w-4 h-4 text-gray-500" />
              </a>
            </div>
          </div>
        </div>
      </div>
    </footer>

    <!-- 全站赏按钮 -->
    <div class="donate-fab-global">
      <button class="donate-fab-btn" @click="showGlobalDonate = !showGlobalDonate" aria-label="打赏支持">
        <span class="donate-fab-btn-text">賞</span>
      </button>
      <Transition name="donate-popup">
        <div v-if="showGlobalDonate" class="donate-fab-popup">
          <button class="donate-popup-close" @click="showGlobalDonate = false"><X class="h-3.5 w-3.5" /></button>
          <div class="donate-popup-inner">
            <p class="donate-popup-title">喜欢番舍的陪伴吗？</p>
            <div class="donate-qr-wrap">
              <img :src="globalQrUrl" alt="支付二维码" class="donate-qr-img" />
            </div>
            <p class="donate-popup-desc">
              想变成萤火会员请联系站长<br>
              <span class="donate-popup-price">¥9.9/月 · 每小时100次AIGC对话</span>
            </p>
          </div>
        </div>
      </Transition>
    </div>
  </div>
</template>

<!-- ═══════════════════════════════════════════════════════════════
     SCRIPT
     ═══════════════════════════════════════════════════════════════ -->

<script setup lang="ts">
import {
  Sparkles,
  Search,
  UserPlus,
  Menu,
  X,
  Heart,
  Home,
  Tv,
  Bot,
  Trophy,
  CalendarCheck2,
  Users,
  MessageCircle,
  Github,
  Twitter,
  AlertCircle,
  ChevronDown,
  UserRoundCog,
  FileText,
  LogOut,
} from 'lucide-vue-next'

const route = useRoute()
const userStore = useUserStore()
const dailyStore = useDailyStore()
const { isOpen: authModalOpen, close: closeAuthModal } = useAuthModal()
const scrolled = ref(false)
const mobileOpen = ref(false)
const fireflyBurst = ref(false)
const loginError = ref('')
const userMenuOpen = ref(false)
const showProfileModal = ref(false)
const showGlobalDonate = ref(false)
const globalQrUrl = ref('/images/douyin-qr.png')
const showGlobalSearch = ref(false)
const globalSearchQuery = ref('')
const globalSearchInputRef = ref<HTMLInputElement | null>(null)
const searchSuggestions = ['芙莉莲', '恋爱', '治愈', '热血']
let fireflyTimer: ReturnType<typeof setTimeout> | undefined

// 导航项
const navItems = [
  { name: '首页', path: '/',          icon: Home },
  { name: '番剧', path: '/anime',     icon: Tv },
  { name: '排行', path: '/ranking',   icon: Trophy },
  { name: '每日', path: '/daily',     icon: CalendarCheck2 },
  { name: 'AIGC', path: '/agent',     icon: Bot },
  { name: '社区', path: '/community', icon: Users },
]

// 滚动监听
onMounted(() => {
  userStore.restoreSession()
  dailyStore.hydrate()
  dailyStore.startBrowsingSession()

  const handleScroll = () => {
    scrolled.value = window.scrollY > 15
  }
  window.addEventListener('scroll', handleScroll, { passive: true })
  window.addEventListener('keydown', handleGlobalKeydown)
  handleScroll()
})

onBeforeUnmount(() => {
  dailyStore.stopBrowsingSession()
  window.removeEventListener('keydown', handleGlobalKeydown)
  if (fireflyTimer) clearTimeout(fireflyTimer)
})

function openGlobalSearch() {
  mobileOpen.value = false
  showGlobalSearch.value = true
  nextTick(() => globalSearchInputRef.value?.focus())
}

function closeGlobalSearch() {
  showGlobalSearch.value = false
}

async function submitGlobalSearch() {
  const query = globalSearchQuery.value.trim()
  if (!query) return
  closeGlobalSearch()
  await navigateTo({ path: '/anime', query: { q: query, focus: 'search' } })
}

function searchByKeyword(keyword: string) {
  globalSearchQuery.value = keyword
  submitGlobalSearch()
}

function handleGlobalKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape' && showGlobalSearch.value) closeGlobalSearch()
}

function openAuthingGuard() {
  if (userStore.isLoggedIn) return
  mobileOpen.value = false
  loginError.value = ''
  authModalOpen.value = true
}

function closeAuthingGuard() {
  authModalOpen.value = false
  loginError.value = ''
}

/** 自定义登录表单回调 */
function handleAuthingLogin() {
  loginError.value = ''
  closeAuthingGuard()
  triggerFireflyBurst()
}

function triggerFireflyBurst() {
  fireflyBurst.value = false
  nextTick(() => {
    fireflyBurst.value = true
    if (fireflyTimer) clearTimeout(fireflyTimer)
    fireflyTimer = setTimeout(() => {
      fireflyBurst.value = false
    }, 1600)
  })
}

async function handleProfileComplete(data: { name: string; avatar: string }) {
  await userStore.completeProfile(data)
  showProfileModal.value = false
  console.log('[Profile] 资料完善完成:', data)
}

async function handleLogout() {
  await userStore.logout()
  userMenuOpen.value = false
  mobileOpen.value = false
  showProfileModal.value = false
  navigateTo('/')
}

// 路由切换时关闭移动菜单
watch(() => route.path, () => {
  mobileOpen.value = false
  showGlobalSearch.value = false
})
</script>

<!-- ═══════════════════════════════════════════════════════════════
     STYLE · 局部微调
     ═══════════════════════════════════════════════════════════════ -->

<style scoped>
/* 导航栏过渡 */
nav {
  transition-property: background, backdrop-filter, box-shadow, border-radius;
  transition-duration: 500ms;
  transition-timing-function: cubic-bezier(0.16, 1, 0.3, 1);
}

/* 全站赏按钮 */
.donate-fab-global {
  position: fixed;
  bottom: 1.5rem;
  right: 1.5rem;
  z-index: 70;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 0.75rem;
}
.donate-fab-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 3rem;
  height: 3rem;
  border-radius: 999px;
  border: 1.5px solid rgba(255, 255, 255, 0.7);
  background: linear-gradient(135deg, rgba(253, 251, 247, 0.92), rgba(240, 244, 248, 0.9));
  backdrop-filter: blur(12px);
  box-shadow: 0 4px 18px rgba(160, 216, 239, 0.22), 0 1px 4px rgba(0, 0, 0, 0.06);
  cursor: pointer;
  transition: all 0.25s ease;
}
.donate-fab-btn:hover {
  transform: scale(1.06);
  box-shadow: 0 6px 24px rgba(160, 216, 239, 0.3), 0 2px 8px rgba(0, 230, 118, 0.15);
}
.donate-fab-btn-text {
  font-size: 1.2rem;
  font-weight: 900;
  color: #374151;
  letter-spacing: 0.02em;
}
.donate-fab-popup {
  width: 18rem;
  overflow: hidden;
  border-radius: 1rem;
  border: 1px solid rgba(255, 255, 255, 0.7);
  background: rgba(255, 250, 240, 0.95);
  box-shadow: 0 20px 60px rgba(31, 68, 47, 0.18);
  backdrop-filter: blur(24px);
  position: relative;
}
.donate-popup-close {
  position: absolute;
  right: 0.75rem;
  top: 0.75rem;
  z-index: 10;
  display: flex;
  width: 1.75rem;
  height: 1.75rem;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.7);
  color: #9ca3af;
  transition: color 0.15s;
}
.donate-popup-close:hover { color: #374151; }
.donate-popup-inner { padding: 1.25rem; text-align: center; }
.donate-popup-title { font-size: 0.875rem; font-weight: 900; color: #1f2937; }
.donate-qr-wrap {
  margin: 1rem auto 0;
  display: flex;
  width: 11rem;
  height: 11rem;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border-radius: 1rem;
  border: 1px solid rgba(255, 255, 255, 0.8);
  background: #fff;
  padding: 0.75rem;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
}
.donate-qr-img { width: 100%; height: 100%; border-radius: 0.75rem; object-fit: cover; }
.donate-popup-desc {
  margin-top: 0.75rem;
  font-size: 0.75rem;
  font-weight: 700;
  color: #6b7280;
  line-height: 1.6;
}
.donate-popup-price { color: #00C853; font-weight: 900; }

.donate-popup-enter-active { transition: all 0.28s cubic-bezier(0.34, 1.56, 0.64, 1); }
.donate-popup-leave-active { transition: all 0.2s ease-in; }
.donate-popup-enter-from { opacity: 0; transform: translateY(12px) scale(0.95); }
.donate-popup-leave-to { opacity: 0; transform: translateY(8px) scale(0.97); }
</style>
