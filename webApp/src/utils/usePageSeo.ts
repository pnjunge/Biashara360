import { useEffect } from 'react'

export interface PageSeoOptions {
  title: string
  description?: string
  canonicalUrl?: string
  ogType?: 'website' | 'article' | 'product'
  ogImage?: string
  keywords?: string
}

function updateMetaTag(attrName: 'name' | 'property', attrValue: string, content: string) {
  let element = document.querySelector(`meta[${attrName}="${attrValue}"]`) as HTMLMetaElement | null
  if (!element) {
    element = document.createElement('meta')
    element.setAttribute(attrName, attrValue)
    document.head.appendChild(element)
  }
  element.setAttribute('content', content)
}

function updateCanonical(url: string) {
  let link = document.querySelector('link[rel="canonical"]') as HTMLLinkElement | null
  if (!link) {
    link = document.createElement('link')
    link.setAttribute('rel', 'canonical')
    document.head.appendChild(link)
  }
  link.setAttribute('href', url)
}

export function usePageSeo({
  title,
  description,
  canonicalUrl = 'https://biashara360.co.ke/',
  ogType = 'website',
  ogImage = 'https://biashara360.co.ke/images/app_screenshot.png',
  keywords
}: PageSeoOptions) {
  useEffect(() => {
    // 1. Page Title
    document.title = title

    // 2. Canonical URL
    if (canonicalUrl) {
      updateCanonical(canonicalUrl)
    }

    // 3. Meta Description
    if (description) {
      updateMetaTag('name', 'description', description)
      updateMetaTag('property', 'og:description', description)
      updateMetaTag('name', 'twitter:description', description)
    }

    // 4. Meta Keywords
    if (keywords) {
      updateMetaTag('name', 'keywords', keywords)
    }

    // 5. OpenGraph & Twitter Titles
    updateMetaTag('property', 'og:title', title)
    updateMetaTag('name', 'twitter:title', title)

    // 6. OpenGraph Type & URL
    updateMetaTag('property', 'og:type', ogType)
    if (canonicalUrl) {
      updateMetaTag('property', 'og:url', canonicalUrl)
    }

    // 7. OpenGraph & Twitter Images
    if (ogImage) {
      updateMetaTag('property', 'og:image', ogImage)
      updateMetaTag('name', 'twitter:image', ogImage)
    }
  }, [title, description, canonicalUrl, ogType, ogImage, keywords])
}
