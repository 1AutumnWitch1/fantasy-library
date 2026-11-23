import { createPinia } from 'pinia'
import { createApp } from 'vue'
import App from './App.vue'
import { router } from './router'
import { configureSiteTheme } from './api/siteMedia'
import './styles/main.scss'
import './styles/community.scss'

configureSiteTheme(document.documentElement.style)
createApp(App).use(createPinia()).use(router).mount('#app')
