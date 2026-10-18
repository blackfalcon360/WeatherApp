package com.blackfalcon.weather;

import java.util.HashMap;
import java.util.Map;

/** All texts in English, Urdu and Arabic. */
public final class Lang {
    private Lang() { }

    public static final int EN = 0, UR = 1, AR = 2;
    private static final Map<String, String[]> T = new HashMap<>();

    private static void a(String k, String en, String ur, String ar) { T.put(k, new String[]{en, ur, ar}); }

    static {
        a("tab_wx", "Weather", "موسم", "الطقس");
        a("tab_places", "Places", "مقامات", "الأماكن");
        a("tab_set", "Settings", "ترتیبات", "الإعدادات");
        a("my_loc", "My location", "میرا مقام", "موقعي");
        a("feels", "Feels like", "محسوس ہوتا ہے", "المحسوس");
        a("hi", "H", "اعلیٰ", "العظمى");
        a("lo", "L", "ادنیٰ", "الصغرى");
        a("now", "Now", "ابھی", "الآن");
        a("next24", "Next 24 hours", "اگلے 24 گھنٹے", "الـ 24 ساعة القادمة");
        a("chart", "Temperature and chance of rain", "درجہ حرارت اور بارش کا امکان", "الحرارة واحتمال المطر");
        a("days7", "7-day forecast", "7 دن کی پیش گوئی", "توقعات 7 أيام");
        a("today", "Today", "آج", "اليوم");
        a("tomorrow", "Tomorrow", "کل", "غدًا");
        a("wind", "Wind", "ہوا", "الرياح");
        a("gusts", "gusts", "جھکڑ", "هبات");
        a("humidity", "Humidity", "نمی", "الرطوبة");
        a("dew", "Dew point", "نقطۂ شبنم", "نقطة الندى");
        a("pressure", "Pressure", "دباؤ", "الضغط");
        a("uv", "UV index", "یو وی انڈیکس", "مؤشر الأشعة فوق البنفسجية");
        a("vis", "Visibility", "نظر آنے کی حد", "مدى الرؤية");
        a("cloud", "Cloud cover", "بادل", "الغيوم");
        a("precip", "Precipitation", "بارش", "هطول الأمطار");
        a("chance", "chance", "امکان", "احتمال");
        a("aqi", "Air quality", "ہوا کا معیار", "جودة الهواء");
        a("sunrise", "Sunrise", "طلوعِ آفتاب", "الشروق");
        a("sunset", "Sunset", "غروبِ آفتاب", "الغروب");
        a("daylen", "Daylight", "دن کا دورانیہ", "طول النهار");
        a("updated", "Updated", "اپ ڈیٹ", "آخر تحديث");
        a("offline", "No internet \u2014 showing saved weather from", "انٹرنیٹ نہیں \u2014 محفوظ شدہ موسم، وقت", "لا إنترنت \u2014 عرض طقس محفوظ من");
        a("loading", "Updating\u2026", "اپ ڈیٹ ہو رہا ہے\u2026", "جارٍ التحديث\u2026");
        a("no_data", "No weather yet. Check your internet and press refresh.", "ابھی موسم کا ڈیٹا نہیں۔ انٹرنیٹ دیکھیں اور ریفریش دبائیں۔", "لا توجد بيانات بعد. تحقق من الإنترنت واضغط تحديث.");
        a("need_perm", "Allow location to see your local weather, or search a city in Places.", "اپنے علاقے کے موسم کے لیے مقام کی اجازت دیں، یا مقامات میں شہر تلاش کریں۔", "اسمح بالموقع لرؤية طقس منطقتك، أو ابحث عن مدينة في الأماكن.");
        a("wait_loc", "Waiting for your location\u2026", "آپ کے مقام کا انتظار ہے\u2026", "بانتظار تحديد موقعك\u2026");
        a("rain_at", "Rain likely around", "بارش کا امکان تقریباً", "المطر محتمل نحو");
        a("snow_at", "Snow likely around", "برف باری کا امکان تقریباً", "الثلج محتمل نحو");
        a("storm_at", "Thunderstorm likely around", "گرج چمک کا امکان تقریباً", "عاصفة رعدية محتملة نحو");
        a("no_rain", "No rain expected in the next 24 hours", "اگلے 24 گھنٹوں میں بارش کی توقع نہیں", "لا مطر متوقع خلال 24 ساعة");
        a("search_hint", "Search city", "شہر تلاش کریں", "ابحث عن مدينة");
        a("search", "Search", "تلاش", "بحث");
        a("saved", "Saved places", "محفوظ مقامات", "الأماكن المحفوظة");
        a("results", "Results (tap to add)", "نتائج (شامل کرنے کے لیے دبائیں)", "النتائج (اضغط للإضافة)");
        a("no_results", "No place found", "کوئی مقام نہیں ملا", "لا توجد نتائج");
        a("use_gps", "Use my location (GPS)", "میرا مقام استعمال کریں (GPS)", "استخدم موقعي (GPS)");
        a("lang", "Language", "زبان", "اللغة");
        a("theme", "Theme", "تھیم", "المظهر");
        a("th_auto", "Auto (system)", "خودکار (سسٹم)", "تلقائي (النظام)");
        a("th_light", "Light", "روشن", "فاتح");
        a("th_dark", "Dark", "تاریک", "داكن");
        a("units", "Units", "اکائیاں", "الوحدات");
        a("metric", "Metric", "میٹرک", "متري");
        a("imperial", "Imperial", "امپیریل", "إمبراطوري");
        a("units_note", "Metric: \u00B0C, km/h, mm, hPa.  Imperial: \u00B0F, mph, in, inHg.", "میٹرک: \u00B0C، کلومیٹر/گھنٹہ، ملی میٹر، hPa۔  امپیریل: \u00B0F، میل/گھنٹہ، انچ، inHg۔", "متري: \u00B0م، كم/س، ملم، hPa.  إمبراطوري: \u00B0ف، ميل/س، بوصة، inHg.");
        a("tf", "Time format", "وقت کا انداز", "تنسيق الوقت");
        a("h12", "12-hour", "12 گھنٹے", "12 ساعة");
        a("h24", "24-hour", "24 گھنٹے", "24 ساعة");
        a("am", "AM", "صبح", "ص");
        a("pm", "PM", "شام", "م");
        a("source", "Data: Open-Meteo.com \u2014 forecast models (ECMWF, NOAA GFS, DWD ICON and others), air quality from CAMS. This is a model forecast, not a reading from a weather station.",
                "ڈیٹا: Open-Meteo.com \u2014 پیش گوئی کے ماڈل (ECMWF، NOAA GFS، DWD ICON وغیرہ)، ہوا کا معیار CAMS سے۔ یہ ماڈل پر مبنی پیش گوئی ہے، کسی موسمی اسٹیشن کی پیمائش نہیں۔",
                "المصدر: Open-Meteo.com \u2014 نماذج التنبؤ (ECMWF وNOAA GFS وDWD ICON وغيرها)، جودة الهواء من CAMS. هذا تنبؤ بالنماذج وليس قراءة محطة أرصاد.");
        a("kmh", "km/h", "کلومیٹر/گھنٹہ", "كم/س");
        a("mph", "mph", "میل/گھنٹہ", "ميل/س");
        a("mm", "mm", "ملی میٹر", "ملم");
        a("in", "in", "انچ", "بوصة");
        a("hpa", "hPa", "hPa", "hPa");
        a("inhg", "inHg", "inHg", "inHg");
        a("km", "km", "کلومیٹر", "كم");
        a("mi", "mi", "میل", "ميل");
        a("uv0", "Low", "کم", "منخفض");
        a("uv1", "Moderate", "درمیانہ", "متوسط");
        a("uv2", "High", "زیادہ", "مرتفع");
        a("uv3", "Very high", "بہت زیادہ", "مرتفع جدًا");
        a("uv4", "Extreme", "انتہائی", "شديد");
        a("aq0", "Good", "اچھا", "جيد");
        a("aq1", "Moderate", "درمیانہ", "متوسط");
        a("aq2", "Unhealthy for sensitive groups", "حساس افراد کے لیے مضر", "غير صحي للفئات الحساسة");
        a("aq3", "Unhealthy", "مضرِ صحت", "غير صحي");
        a("aq4", "Very unhealthy", "بہت مضرِ صحت", "غير صحي جدًا");
        a("aq5", "Hazardous", "خطرناک", "خطير");
        a("c_clear", "Clear sky", "صاف آسمان", "سماء صافية");
        a("c_mainly", "Mainly clear", "زیادہ تر صاف", "صافٍ غالبًا");
        a("c_partly", "Partly cloudy", "جزوی ابر آلود", "غائم جزئيًا");
        a("c_overcast", "Overcast", "گھنے بادل", "غائم كليًا");
        a("c_fog", "Fog", "دھند", "ضباب");
        a("c_drizzle", "Drizzle", "ہلکی پھوار", "رذاذ");
        a("c_fdrizzle", "Freezing drizzle", "جمنے والی پھوار", "رذاذ متجمد");
        a("c_rain_l", "Light rain", "ہلکی بارش", "مطر خفيف");
        a("c_rain", "Rain", "بارش", "مطر");
        a("c_rain_h", "Heavy rain", "تیز بارش", "مطر غزير");
        a("c_frain", "Freezing rain", "جمنے والی بارش", "مطر متجمد");
        a("c_snow_l", "Light snow", "ہلکی برف باری", "ثلج خفيف");
        a("c_snow", "Snow", "برف باری", "ثلج");
        a("c_snow_h", "Heavy snow", "شدید برف باری", "ثلج كثيف");
        a("c_grains", "Snow grains", "برف کے دانے", "حبيبات ثلجية");
        a("c_show_l", "Light showers", "ہلکی بوچھاڑ", "زخات خفيفة");
        a("c_show", "Rain showers", "بارش کی بوچھاڑ", "زخات مطر");
        a("c_show_h", "Violent showers", "شدید بوچھاڑ", "زخات عنيفة");
        a("c_snowshow", "Snow showers", "برف کی بوچھاڑ", "زخات ثلج");
        a("c_thunder", "Thunderstorm", "گرج چمک کے ساتھ بارش", "عاصفة رعدية");
        a("c_hail", "Thunderstorm with hail", "گرج چمک اور اولے", "عاصفة رعدية مع برد");
    }

    public static final String[] DIR_EN = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
    public static final String[] DIR_UR = {"شمال", "شمال مشرق", "مشرق", "جنوب مشرق", "جنوب", "جنوب مغرب", "مغرب", "شمال مغرب"};
    public static final String[] DIR_AR = {"شمال", "شمال شرق", "شرق", "جنوب شرق", "جنوب", "جنوب غرب", "غرب", "شمال غرب"};

    public static final String[] WEEK_EN = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
    public static final String[] WEEK_UR = {"اتوار", "پیر", "منگل", "بدھ", "جمعرات", "جمعہ", "ہفتہ"};
    public static final String[] WEEK_AR = {"الأحد", "الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت"};

    public static final String[] MON_EN = {"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};
    public static final String[] MON_UR = {"جنوری", "فروری", "مارچ", "اپریل", "مئی", "جون", "جولائی", "اگست", "ستمبر", "اکتوبر", "نومبر", "دسمبر"};
    public static final String[] MON_AR = {"يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو", "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"};

    public static String t(int lang, String key) {
        String[] v = T.get(key);
        return v == null ? key : v[lang];
    }

    public static String[] pick(int lang, String[] en, String[] ur, String[] ar) { return lang == UR ? ur : (lang == AR ? ar : en); }
}
