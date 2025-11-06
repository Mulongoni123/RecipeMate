# RecipeMate 🍳

A modern Android recipe application built with Kotlin that helps you discover, save, and plan your meals effortlessly.

![RecipeMate Banner](https://via.placeholder.com/800x200/4CAF50/FFFFFF?text=RecipeMate+-+Your+Personal+Recipe+Companion)

## Features ✨

### 🔍 Smart Recipe Search
- Search from thousands of recipes using Spoonacular API
- Filter by ingredients, cuisine, diet preferences
- Real-time search suggestions

### ❤️ Personalized Favorites
- Save your favorite recipes for quick access
- Sync favorites across devices with Firebase
- Easy one-tap favorite management

### 📅 Meal Planning
- Plan your meals for any date
- Organize by meal types (Breakfast, Lunch, Dinner, Snack)
- Visual calendar interface

### 🛒 Grocery List Generator
- Automatically generate shopping lists from meal plans
- Organize items by category
- Check off items as you shop

### 🔐 Secure Authentication
- Firebase Authentication with email/password
- Secure data encryption
- User profile management

## Screenshots 📱

| Search & Discover | Recipe Details | Meal Planner |
|-------------------|----------------|--------------|
| <img src="https://via.placeholder.com/250x500/4CAF50/FFFFFF?text=Search" width="200"> | <img src="https://via.placeholder.com/250x500/2196F3/FFFFFF?text=Details" width="200"> | <img src="https://via.placeholder.com/250x500/FF9800/FFFFFF?text=Planner" width="200"> |

| Favorites | Grocery List | User Profile |
|-----------|--------------|--------------|
| <img src="https://via.placeholder.com/250x500/E91E63/FFFFFF?text=Favorites" width="200"> | <img src="https://via.placeholder.com/250x500/9C27B0/FFFFFF?text=Grocery" width="200"> | <img src="https://via.placeholder.com/250x500/607D8B/FFFFFF?text=Profile" width="200"> |

## Tech Stack 🛠️

### Frontend
- **Kotlin** - Primary programming language
- **Android Jetpack Components**:
  - ViewModel & LiveData for data management
  - Room Database for local storage
  - Navigation Component for fragment management
  - RecyclerView for efficient lists
- **Material Design 3** for modern UI components

### Backend & APIs
- **Firebase**:
  - Authentication for user management
  - Firestore for cloud data sync
  - Storage for user content
- **Spoonacular API** - Recipe data provider
- **Retrofit 2** - HTTP client for API calls

### Architecture & Patterns
- **MVVM (Model-View-ViewModel)** Architecture
- **Repository Pattern** for data abstraction
- **Coroutines** for asynchronous programming
- **Dependency Injection** with Hilt

## Project Structure 📁
