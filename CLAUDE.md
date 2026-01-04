# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

A GitHub Pages blog frontend using ClojureScript (shadow-cljs) with Reagent for UI and Vite for asset bundling.

## Common Commands

```bash
# Development (starts Vite + shadow-cljs watch)
pnpm dev

# Production build
pnpm build

# Publish to GitHub Pages
pnpm p
```

## Architecture

**Build System:**
- **Vite**: Dev server and production bundler
- **shadow-cljs**: Compiles ClojureScript, integrated via `vite-plugin-shadow-cljs`

**Source Directories:**
- `cljs/` - ClojureScript source (entry: `browser.core`)
- `scripts/` - JS entry point (imports styles)
- `styles/` - SCSS styles
- `assets/` - Static files (index.html, CNAME)
- `configs/` - Build configuration (vite-plugin-shadow-cljs)

**ClojureScript Architecture (`cljs/browser/`):**
- `core.cljs` - Main app: Redux-like state management with Reagent, async action handling via core.async channels
- `github.cljs` - GitHub API integration for fetching blog posts
- `utils.cljs` - DOM utilities, markdown rendering

**State Management:**
Uses a custom Redux-like pattern with:
- Multimethod-based reducer (`defmulti reducer`)
- Channel-based middleware for async actions
- Reagent atoms for reactive UI updates

**Key Dependencies:**
- Reagent (React wrapper)
- rxcljs (async utilities)
- Redux-like custom implementation in `redux/` namespace
