# Delay Watcher
Delay Watcher is a sleek, modern Android application and Home Screen Widget designed to keep you updated on real-time train disruptions across the UK National Rail network. Inspired by modern transit apps like TfL Go, it delivers clean, perfectly aligned disruption alerts right to your home screen.

## Features
* **Live Disruption Widget**: A beautifully aligned, customisable widget that sits on your home screen and updates seamlessly.
* **TOC Filtering**: Filter disruptions by your specific Train Operating Companies (e.g., Thameslink, Southeastern, Southern, London Overground).
* **Smart Caching & Syncing**: Minimises battery drain and API calls. The widget updates instantly when you modify filters or refresh the main app, avoiding background crash loops.
* **Detailed Incident Reports**: Tap the widget to open the app and read full, clickable HTML incident reports directly from National Rail.
* **Dynamic Branding**: UI elements dynamically tint to match the official brand colors of the affected train operators.

## Tech Stack
* **Language**: Java
* **Platform**: Android SDK
* **Networking**: Retrofit2 & Gson
* **Data Storage**: SharedPreferences (Local Caching)
* **API**: National Rail Disruptions Experience API

## Obtaining a National Rail API Key
To use this app, you need a free API token from National Rail:
1. Go to the [Rail Data Marketplace](https://raildata.org.uk).
2. Click **Register** and create a free account.
3. Once logged in, navigate to your **My Account** or **Subscriptions** dashboard.
4. Look for the **Disruptions Experience API** (or equivalent real-time incidents API) and click **Subscribe**.
5. Once subscribed, you will be issued a **Customer Key** (Token).
6. Copy this key and paste it into the Settings screen inside the Delay Watcher app.

## Setup & Installation
1. Download the APK from the latest release
2. On the first launch, tap the settings spanner to enter your **Customer Key** and select your tracked TOCs.
3. Add the **Delay Watcher** widget to your Android home screen!

## Acknowledgements
Data provided by the [National Rail Open Data API](https://opendata.nationalrail.co.uk/).

## Credits
Assets: The app icon was generated using AI.