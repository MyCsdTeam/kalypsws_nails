# Kalypsws Nails - Android Application

Welcome to the **Kalypsws Nails** repository! This is an Android application designed to help manage and serve clients for a beauty and nail salon.

## 💅 What the App Does

* **Appointments:** Clients can easily book, reschedule, or cancel beauty treatments.
* **Service Catalog:** Displays a structured menu of salon services, along with descriptions, prices, and durations.
* **Notifications:** Uses cloud messaging to send push notifications for appointment reminders and salon promotions.

## 🚀 Getting Started

To run this project locally on your machine, you will need:
* **Android Studio** (Latest version recommended)
* **Android SDK**
* **Java Development Kit (JDK)**

### Build Instructions
This project uses the Gradle Build System. You don't need to install Gradle manually; just use the included wrapper scripts in your terminal:

**For Windows:**
gradlew.bat build

**For macOS / Linux:**
./gradlew build

## 🔒 Security Policies & Git Best Practices

To safeguard production endpoints and keep the codebase clean, a strict `.gitignore` policy is enforced across the workspace. The following components are barred from being tracked or pushed to remote repositories:

* **Compilation Outputs:** The entire `/build` directory, temporary caches, and final distribution packages (`*.apk`, `*.aab`).
* **IDE & Workplace States:** Project metadata, workspace preferences, caching layers, and system files (`.idea/`, `.gradle`, `.DS_Store`, `*.iml`, `/captures`, `.externalNativeBuild`, `.cxx`).
* **Protected Secrets & Keyrings:**
    * `google-services.json`: Contains private Firebase infrastructure IDs, API tokens, and cloud client parameters.
    * `**/secrets.xml`: A dedicated security file for local app credentials, merchant credentials, and custom private string resources.

> [cite_start]⚠️ **Important for Developers:** Since `google-services.json` and `secrets.xml` are omitted from the repository for vital security reasons, you must obtain these active credential blocks directly from the project administrator and position them in their respective module pathways (e.g., inside the `/app` directory) before attempting to assemble a release version of the application