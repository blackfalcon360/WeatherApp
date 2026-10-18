# Weather ☀️🌧️

A clean weather app with **light and dark themes**, in **Urdu, English and Arabic** (Urdu/Arabic are right-to-left). Portrait only.

## What it shows
- **Now**: temperature, feels-like, condition with a drawn weather picture, today's high/low and a one-line outlook ("Rain likely around 5 PM" / "No rain expected in the next 24 hours").
- **Next 24 hours**: hour by hour strip (picture, temperature, rain chance) and a temperature curve with rain-chance bars.
- **7-day forecast**: day, picture, rain chance, low/high with a range bar.
- **Details**: wind (speed, direction arrow, gusts), humidity and dew point, UV index (with level), pressure, visibility, cloud cover, precipitation (now / today / chance), **air quality** (US AQI with PM2.5 / PM10), sunrise, sunset and daylight length.
- **Places**: use your GPS location, or search any city in the world and save it; switch between saved places.
- **Settings**: language, theme (Auto follows the phone / Light / Dark), units (Metric °C, km/h, mm, hPa — or Imperial °F, mph, in, inHg), 12/24-hour time.

## Where the data comes from (and how accurate it is)
Forecasts come from **Open-Meteo.com**, which blends the leading weather-service models (ECMWF, NOAA GFS, DWD ICON and others) and picks the best model for your location. Air quality comes from CAMS. Times (sunrise, hourly labels) are shown in the **place's own time zone**.
This is a *model forecast*, not a reading from a weather station, so the real temperature on your street can differ by a degree or two, and rain timing can shift by some hours.

The last forecast is saved on the phone, so without internet you still see it, marked with the time it was updated. It refreshes by itself every 20 minutes while the app is open.

Needs the Internet permission (and Location only if you use GPS). Android 7.0 (API 24) or newer.

Brought to you By: Black Falcon 🦅 (shown in the lower right corner)

## Build
Push to GitHub -> Actions -> "Build APK" -> download `WeatherApp-APK`.
Local: `gradle assembleDebug` (JDK 17, Gradle 8.7+).
