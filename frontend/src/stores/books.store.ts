import { ref } from 'vue'
import { defineStore } from 'pinia'
import { booksApi } from '@/api/books.api'
import type { Book } from '@/types/domain'

export const useBooksStore = defineStore('books', () => {
  const books = ref<Book[]>([])
  const categories = ref<string[]>([])

  const loadCategories = async () => {
    if (!categories.value.length) categories.value = await booksApi.categories()
    return categories.value
  }

  const load = async (options: { keyword?: string; category?: string } = {}) => {
    const keyword = options.keyword?.trim()
    books.value = keyword
      ? await booksApi.search(keyword, options.category)
      : await booksApi.list(options.category)
    return books.value
  }

  return { books, categories, load, loadCategories }
})
