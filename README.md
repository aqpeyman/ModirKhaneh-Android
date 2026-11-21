# مدیرخانه — Native Android

نسخه توسعه: `0.2.0-alpha` — Kotlin، Jetpack Compose، Room/SQLite، حداقل Android API 26 و Java 17.

## قابلیت‌های متصل‌شده به Room
- ثبت و فهرست واحدها و ساکنین پایه
- تعریف بازه شارژ با کنترل هم‌پوشانی
- ثبت پرداخت با کنترل وجود شارژ و مانده در یک تراکنش SQLite
- ثبت هزینه‌های ساختمان
- خلاصه آماری اولیه

## نکات مهم
- شماره تلفن در مدل داده String است تا صفر ابتدایی حفظ شود.
- ثبت شارژ باید از `AppDatabase.addChargePeriod` و پرداخت از `AppDatabase.registerPayment` انجام شود.
- دستیار در حال حاضر فقط صفحه معرفی دارد؛ موتور llama.cpp و مدل GGUF هنوز متصل نیستند.
- فایل مدل در این مخزن وجود ندارد و APK مستقل همراه مدل هنوز ساخته نشده است.
- پشتیبان‌گیری خودکار اندروید غیرفعال است تا داده‌های محلی مالی بدون تصمیم کاربر به فضای پشتیبان سیستم منتقل نشوند.

## ساخت در Android Studio
پروژه را در Android Studio باز کنید، JDK 17 و Android SDK 35 را نصب کنید، Gradle Sync را انجام دهید و سپس از مسیر Build > Build Bundle(s) / APK(s) > Build APK(s) خروجی آزمایشی بگیرید.

## وضعیت آزمون
در محیط تهیه این بسته Android SDK و Gradle موجود نبود؛ بنابراین این نسخه هنوز کامپایل یا روی دستگاه اجرا نشده است. پیش از انتشار، تست‌های JVM، تست Room روی دستگاه و آزمون UI باید اجرا شوند.

برای جزئیات به `IMPLEMENTATION_PROGRESS_FA.md` و `ANDROID_BUILD_STATUS.md` مراجعه کنید.

## ساخت APK بدون Android Studio
برای ساخت ابری با GitHub Actions، راهنمای `GITHUB_BUILD_FA.md` را دنبال کنید. Workflow در مسیر `.github/workflows/android-apk.yml` قرار دارد و پس از push یا اجرای دستی، APK آزمایشی را به‌صورت Artifact منتشر می‌کند.
