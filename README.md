## Лабораторна робота №3: Анотації
Предметна область: облік книг (Book, Author), статус читання (PLANNED, READING, FINISHED) і прогрес читання (totalPages, pagesRead).

У проєкті реалізовано три кастомні анотації, кожна з унікальним механізмом:

№ 1
Тип: Bean Validation constraint
Анотація: @ValidReadingProgress
Де застосована: BookDto (рівень класу)
Реалізація: validation/ReadingProgressValidator

№ 2
Тип: Композитна (meta-annotation)
Анотація: @PostCreated = @RequestMapping(POST) + @ResponseStatus(CREATED)
Де застосована: BookController.create
Реалізація: annotation/PostCreated

№ 3
Тип: HandlerMethodArgumentResolver
Анотація: @CurrentUsername
Де застосована: параметр BookController.create
Реалізація: config/CurrentUsernameArgumentResolver, реєстрація в config/WebConfig

Деталі реалізації анотацій:

а) @ValidReadingProgress (Bean Validation)
Нове бізнес-правило для BookDto, що перевіряє узгодженість трьох полів:
- pagesRead не можна вказати без totalPages.
- pagesRead не може перевищувати totalPages.
- Книга зі статусом PLANNED не може мати прочитаних сторінок.
- Книга зі статусом FINISHED має мати pagesRead == totalPages.

б) @PostCreated (Композитна мета-анотація)
Замінює пару @PostMapping + @ResponseStatus(HttpStatus.CREATED) однією анотацією (аналогічно до @RestController). 
Базою взято @RequestMapping(method = POST), оскільки стандартний @PostMapping не підтримується як мета-анотація. 

в) @CurrentUsername (Argument Resolver)
Параметр типу String, позначений цією анотацією, автоматично отримує ім'я поточного користувача (claim preferred_username).
У методі BookController.create значення записується в dto.setCreatedBy(username), що захищає систему від спробуи клієнта надіслати підроблене поле createdBy у тілі запиту.

Покриття тестів (AC):

AC 1-3 (ReadingProgressValidatorTest, BookControllerTest): Перевірка валідатора, коректних станів без помилок та генерації 400 Bad Request із чіткими повідомленнями при порушенні бізнес-логіки.

AC 4-5 (BookControllerTest): Перевірка мета-анотації @PostCreated, коректності статусів (201 Created) та маршрутизації (@WebMvcTest).

AC 6-7 (CurrentUsernameArgumentResolverTest): Перевірка резолвера для JWT, OIDC, звичайного Principal та обробки відсутності авторизації.

AC 8 (BookControllerTest): Перевірка того, що навіть якщо клієнт надсилає createdBy: "hacker", у сервісі та у відповіді записується реальний авторизований користувач.


# Лабораторна робота №4: AOP
Тема: Aspect Oriented Programming (Spring AOP). 
Реалізовно у пакеті com.booktracker.booktracker.aop.

1. Анотації та аспекти

Усі три анотації мають @Retention(RUNTIME) і @Target(METHOD), а аспекти знаходять їх через pointcut @annotation(...).

a) @Audited(action) (AuditedAspect / @AfterReturning) — після успішного виклику записує в AuditTrail і лог такі дані: користувач, дію, метод, id ресурсу.
Де застосовано: BookService.create, BookService.delete.

b) @RateLimited(maxCalls, perSeconds) (RateLimitedAspect / @Before) — рахує виклики користувача у ковзному вікні. Якщо ліміт перевищено, кидає TooManyRequestsException (HTTP 429).
Де застосовано: AuthorService.create (5 викликів за 60 с).

c) @RetryOnTransientFailure(maxAttempts) (RetryOnTransientFailureAspect / @Around) — повторює виклик, якщо виникла TransientDataAccessException. Після вичерпання спроб кидає останню помилку.
Де застосовано: BookService.create (3 спроби).


2. Self-invocation

Метод BookService.create позначений @Audited і @RetryOnTransientFailure. Його можна викликати двома способами:

a) Перший випадок (успішний шлях):

Шлях виклику: POST /api/books → BookController → bookService.create(...)

Через проксі?: Так.

Результат: Є запис AUDIT ... BOOK_CREATED, працює retry.

b) Другий випадок (проблемний шлях):

Шлях виклику: POST /api/books/bulk → bookService.createAll(...) → this.create(...)

Через проксі?: Ні.

Результат: Книги створюються, але аудиту й retry немає, помилок теж немає.   

Причина: Spring AOP працює через проксі-об'єкт. Виклик this.create() усередині класу іде повз проксі, тому advice не спрацьовує. Це навмисна частина вправи, а не помилка.

Як перевірити вручну:
Виконати POST /api/books/bulk: у консолі після insert into book немає рядка AUDIT.
<img width="1086" height="723" alt="Книги id 1 і 2 аудит-запису в консолі немає, бо createAll викликає create повз проксі" src="https://github.com/user-attachments/assets/84dd38ed-84f1-43e2-bbf6-137e053698d2" />

Виконати POST /api/books: у консолі з'являється AUDIT user=admin-user action=BOOK_CREATED method=BookService.create(..) resourceId=....
<img width="958" height="113" alt="аудит-запис BOOK_CREATED (user=admin-user, resourceId=1)" src="https://github.com/user-attachments/assets/fb25304d-0dbe-4a96-afd3-411733449e96" />

3. Тести
   
a) Unit-тести advice (AuditedAspectTest, RateLimitedAspectTest, RetryOnTransientFailureAspectTest):

     тестують логіку із замокованими JoinPoint та ProceedingJoinPoint без підняття Spring-контексту.
   
b) AopAnnotationsContractTest:

     перевіряє, що всі анотації мають RUNTIME retention та METHOD target.
     
c) SelfInvocationTest:

     зовнішній виклик create спрацьовує через проксі (аудит, retry), а createAll → this.create ні

Запуск: mvn test

4. Rate limit: перевірка
Шість запитів поспіль до POST /api/authors: перші п'ять повертають OK, шостий повертає 429. Ліміт скидається через 60 секунд.
<img width="1823" height="220" alt="вивід із 429 на шостому запиті" src="https://github.com/user-attachments/assets/b7aa4593-454d-4a93-bec8-0488dea12e6f" />


## Лабораторна робота №5: Асинхронна обробка

Тема: Асинхронна обробка (@Async, CompletableFuture, ThreadPoolTaskExecutor). Реалізовано у пакеті com.booktracker.booktracker.async.

Виконавець: AsyncConfig визначає бін ThreadPoolTaskExecutor (bookTaskExecutor) з явними обмеженнями: базовий пул 2, максимальний 4, черга 10, префікс потоків book-async-. 
Політика відхилення задана явно: AbortPolicy. 
Цей виконавець є виконавцем за замовчуванням для @Async (AsyncConfigurer), тому методи не потрапляють ні в пул Spring Boot за замовчуванням, ні в SimpleAsyncTaskExecutor.
Пул вміщує 4 завдання, що виконуються, і 10 в черзі, тобто 14.

Три повільні побічні ефекти:

Зовнішні сервіси імітуються SimulatedLatency (затримка ~1 с, налаштовується через app.async.simulated-delay-ms).

a) Сповіщення "книгу додано" (NotificationService.sendBookCreatedEmail) — @Async void, "запустив і забув". Викликається з BookController.create через BookCreatedNotifier. Збої в void-методі фіксує RecordingAsyncExceptionHandler (AsyncUncaughtExceptionHandler): пише в лог і запам'ятовує помилку.

b) Рейтинг книги (BookInsightsService.fetchRating) і c) біографія автора (BookInsightsService.fetchAuthorBio) — два незалежні @Async-методи, що повертають CompletableFuture. BookDetailsService.getDetails запускає обидва одразу й об'єднує результати через thenCombine в BookDetailsDto. Ендпоінт: GET /api/books/{id}/details.

Обробка збоїв і часу очікування
- Збій асинхронного виклику: exceptionally повертає null замість винятку, відповідь має partial = true, запит завершується 200, а не помилкою.
- Очікування обмежене: completeOnTimeout (app.async.details-timeout-ms, за замовчуванням 2000 мс). Таймаут зупиняє очікування, а не саму роботу в потоці пулу, тому пул обмежений.

Переповнення виконавця
Коли пул і черга заповнені, Spring кидає TaskRejectedException. Рішення явні:
- GET /api/books/{id}/details: GlobalExceptionHandler повертає 503 Service Unavailable із заголовком Retry-After: 5 і повідомленням "Server is busy, please try again later", а не випадкову 500.
- Сповіщення: BookCreatedNotifier перехоплює виняток, пише попередження в лог і пропускає лист, бо він не критичний і не має провалювати створення книги. try/catch стоїть в окремому біні, оскільки @Async працює через проксі (як і AOP, self-invocation його оминає).

Тести:

a) AsyncConfigTest: потік має префікс book-async-, не default і не SimpleAsyncTaskExecutor, виконавець має явні розміри пулу й черги.

b) NotificationServiceTest: метод повертає керування одразу, завершується пізніше в потоці виконавця; збій у @Async void фіксується AsyncUncaughtExceptionHandler.

c) BookDetailsServiceTest: два виклики виконуються одночасно (CyclicBarrier на двох учасників проходить лише при паралельності) і результати об'єднані; збій одного виклику дає partial-результат; повільний виклик обривається за таймаутом.

d) ExecutorSaturationTest: виконавець із 1 потоком і без черги відхиляє зайве завдання через TaskRejectedException.

e) GlobalExceptionHandlerAsyncTest, BookControllerTest: TaskRejectedException перетворюється на 503 з Retry-After, а не на 500; після створення книги викликається сповіщення з користувачем і назвою.

f) BookCreatedNotifierTest: при переповненні сповіщення пропускається без винятку.


Запуск: mvn test


Перевірка вручну:

A) Fire-and-forget: POST /api/books повернув відповідь за 124 мс, а в консолі приблизно через секунду з'явився рядок Email sent to admin-user: Book added: 1984 (thread book-async-1).
<img width="1836" height="340" alt="Демо A - повільний метод не блокує, POST took 124 ms" src="https://github.com/user-attachments/assets/526a9191-c6c4-4aab-8d55-96bc45d63105" />

B) Паралельність: GET /api/books/1/details виконався за 1033 мс (рейтинг 4.5, partial = False). Якби виклики йшли по черзі, було б понад 2000 мс.
<img width="1815" height="310" alt="Демо B - два виклики паралельно, Elapsed 1033 ms" src="https://github.com/user-attachments/assets/af9d97ad-ebd2-43a8-a945-57848bae622f" />

C) Переповнення: 10 одночасних запитів до /details дали 6 відповідей 200 і 4 відповіді 503, жодної 500.
     демонструє роботу проксі: зовнішній виклик create активує аудит і retry, тоді як внутрішній виклик this.create — ні.  
<img width="1815" height="400" alt="Демо C - переповнення → 503" src="https://github.com/user-attachments/assets/d61c139b-99d8-4461-9dfc-106f9212e0e4" />



## Лабораторна робота №6: Quartz Scheduler

**Тема**: Планування завдань (Quartz Scheduler, JDBC job store, Liquibase). 

Реалізовано у пакетах com.booktracker.booktracker.job, config (QuartzConfig) та service.

**Предметна область**: облік книг. Регулярні завдання виконуються за розкладом, який зберігається в базі даних, тому перезапуск або розгортання нової версії не скасовує й не дублює їх.

&nbsp;

**Завдання (Jobs) і тригери**

* **`ReadingReminderJob`** (Група: `reminders`)
  * **Що робить:** нагадує про книги зі статусом `READING`, про які давно не нагадували
  * **Тригери:**
    * `readingReminderDaily`: щодня о 09:00 (`0 0 9 * * ?`)

* **`ReadingReportJob`** (Група: `reports`)
  * **Що робить:** зберігає знімок статистики (книг усього, по статусах) у таблицю `reading_report`
  * **Тригери:**
    * `readingReportWeekly`: щопонеділка о 08:00 (`0 0 8 ? * MON`)
    * `readingReportMonthly`: 1-го числа о 08:00 (`0 0 8 1 * ?`)

ReadingReportJob має два тригери. Усі cron-тригери працюють у часовому поясі Europe/Kyiv.

Job-класи, JobDetail і Trigger бини оголошені в одному місці: config/QuartzConfig (@Configuration). 

Кожен тригер вказує на завдання через JobKey (forJob(...)), кожне завдання й тригер мають явні name і group.

&nbsp;

**Схема бази даних**

* Схема Quartz (`QRTZ_*`) створюється Liquibase: `db/changelog/changes/003-quartz-schema.yaml` виконує SQL-скрипт `changes/sql/quartz_tables_h2.sql` (офіційна схема Quartz для H2; тип `IMAGE` замінено на `BLOB`).
* Spring Boot налаштований не створювати схему: `spring.quartz.jdbc.initialize-schema: never`.
* `004-reminders-and-reports.yaml` додає колонку `book.last_reminded_at` та таблицю `reading_report` з унікальним обмеженням `(period_type, report_date)`.
* Для збереження розкладу після перезапуску база змінена з `jdbc:h2:mem:booktracker` на файлову: `jdbc:h2:file:./data/booktracker`. Папку `data/` додано в `.gitignore`.

&nbsp;

**Налаштування Quartz (`application.yaml`):**

```yaml
spring:
  quartz:
    job-store-type: jdbc
    jdbc:
      initialize-schema: never
    overwrite-existing-jobs: true
    scheduler-name: booktracker-scheduler
    wait-for-jobs-to-complete-on-shutdown: true
```

overwrite-existing-jobs: true потрібен, щоб зміни розкладу чи параметрів у коді застосовувалися при розгортанні нової версії (інакше Quartz лишає старі визначення з бази).

&nbsp;

**Параметри через JobDataMap**

Кожен JobDetail містить значення за замовчуванням, тригер може перевизначити деякі з них. Quartz об'єднує обидві карти, значення тригера має пріоритет, а ключі, яких у тригері немає, лишаються за замовчуванням.

| Job | Ключ | Значення за замовчуванням | Перевизначення |
| :--- | :--- | :--- | :--- |
| `ReadingReminderJob` | `remindEveryDays` | `7` | немає |
| `ReadingReminderJob` | `batchLimit` | `100` | немає |
| `ReadingReportJob` | `periodType` | `WEEKLY` | `readingReportMonthly` ставить `MONTHLY` |
| `ReadingReportJob` | `minBooks` | `1` | немає (у місячного тригера лишається 1) |

Job читає параметри через JobParams, який кидає явну JobParameterException (з назвою ключа), якщо ключ відсутній або має неправильний тип чи значення. Така помилка вважається помилкою конфігурації, тож повторного запуску не буде.

&nbsp;

**Легкі Job-и**

Job-клас лише читає параметри й робить один виклик сервісу Spring, бізнес-логіки в ньому немає. Межа транзакції проходить на рівні методу сервісу (`@Transactional`):

* `ReadingReminderService.sendReminders(remindEveryDays, batchLimit)`
* `ReadingReportService.generate(periodType, minBooks)`

&nbsp;

**Ідемпотентність**

* **Нагадування.** Після відправки листа сервіс записує `last_reminded_at`. Запит `findDueForReminder` вибирає лише книги зі статусом `READING`, які ніколи не отримували нагадування або отримали його раніше, ніж `remindEveryDays` днів тому. Повторний запуск одразу після першого нічого не знаходить.
* **Звіт.** Перед створенням сервіс перевіряє, чи є звіт для пари (`periodType`, `reportDate`). Додатково унікальне обмеження в БД не дозволяє створити дублікат навіть при гонці.

&nbsp;

**Збої й повторний запуск**

Для двох Job-ів свідомо обрано дві різні стратегії:

| Job | Стратегія | Реалізація |
| :--- | :--- | :--- |
| `ReadingReminderJob` | повтор з обмеженням | при помилці `JobExecutionException(e, true)` (`refireImmediately`), доки `getRefireCount() < MAX_REFIRES` (3); після цього помилка логується й Job чекає наступного cron. Нескінченного циклу немає |
| `ReadingReportJob` | фіксація помилки й очікування | виняток перехоплюється, пишеться `log.error`, наступна спроба буде за наступним розкладом |

&nbsp;

**Паралельність і misfire**

* **Обидва Job-и позначені `@DisallowConcurrentExecution`**: наступний запуск не починається, доки не завершився попередній. Оскільки обидва тригери `ReadingReportJob` вказують на один `JobKey`, вони теж не виконуються одночасно.
* **Для кожного тригера явно задана misfire-інструкція:**

| Тригер | Інструкція | Чому |
| :--- | :--- | :--- |
| `readingReminderDaily` | `DO_NOTHING` | пропущене нагадування наздоганяти не потрібно, наступне прийде за розкладом[cite: 3] |
| `readingReportWeekly`, `readingReportMonthly` | `FIRE_ONCE_NOW` (`FireAndProceed`) | пропущений звіт безпечно виконати одразу, бо Job ідемпотентний[cite: 3] |

&nbsp;

**Тести**

Усього додано **21 новий тест**:

* **AC 1, 2, 5, 6, 7: `QuartzConfigTest`** (звичайний JUnit, без Spring-контексту)
  * тригери вказують на свої Job-и через `JobKey`, `name` і `group` не порожні;
  * значення з тригера перевизначає значення `JobDetail`, а решта ключів лишаються за замовчуванням;
  * `isConcurrentExecutionDisallowed()` для обох Job-ів;
  * misfire-інструкція кожного тригера;
  * ідентифікатор часового поясу `Europe/Kyiv` (невідомий id Quartz мовчки замінює на GMT, тому тест це перевіряє).

* **AC 3, 4, 9: `ReadingReminderJobTest`, `ReadingReportJobTest`** (Mockito)
  * Job читає параметри й викликає сервіс рівно один раз;
  * відсутній або неправильно названий ключ дає `JobExecutionException` без refire;
  * при збої нагадування перезапускається, поки не вичерпано `MAX_REFIRES`, після цього виняток не кидається;
  * збій звіту фіксується й не кидається далі.

* **AC 8: `ReadingReminderServiceIT`, `ReadingReportServiceIT`** (`@DataJpaTest`, H2 + Liquibase)
  * два запуски поспіль: нагадування надсилається один раз, у `reading_report` лишається один рядок;
  * книги не в статусі `READING` і книги без власника ігноруються;
  * повторне нагадування лише після спливу `remindEveryDays`;
  * звіти `WEEKLY` і `MONTHLY` за один день — це різні записи;
  * при кількості книг менше `minBooks` звіт не створюється.

* **AC 10: `QuartzSchemaIT`**
  * таблиці `QRTZ_*` (11 штук) існують завдяки changeset Liquibase;
  * `spring.quartz.jdbc.initialize-schema` дорівнює `never`, `job-store-type` дорівнює `jdbc`.

**Запуск:** `mvn test`

&nbsp;

**Перевірка вручну**

#### A) Розклад у БД
У H2 Console (`jdbc:h2:file:./data/booktracker`):

```sql
SELECT JOB_NAME, JOB_GROUP FROM QRTZ_JOB_DETAILS;
SELECT TRIGGER_NAME, JOB_NAME, TRIGGER_STATE FROM QRTZ_TRIGGERS;
SELECT TRIGGER_NAME, CRON_EXPRESSION, TIME_ZONE_ID FROM QRTZ_CRON_TRIGGERS
```

Результат: 2 завдання, 3 тригери в стані WAITING, у всіх cron-тригерів Europe/Kyiv.

<img width="1009" height="758" alt="2 job-и, 3 тригери в стані WAITING, у всіх cron Europe - Kyiv" src="https://github.com/user-attachments/assets/bd26017b-ef17-45f1-b82b-398ea68f8ce2" />


#### B) Розклад переживає перезапуск
Після повного перезапуску застосунку:

```sql
SELECT (SELECT COUNT(*) FROM QRTZ_JOB_DETAILS) AS jobs,
       (SELECT COUNT(*) FROM QRTZ_TRIGGERS) AS triggers;
```

Результат: 2 і 3, тобто завдання не пропали й не задублювались.

<img width="845" height="371" alt="збереження розкладу після перезапуску" src="https://github.com/user-attachments/assets/cfb5bea3-e466-4d9c-af3e-ccce4ab0939d" />

#### C) Схема створена Liquibase

```sql
SELECT ID, FILENAME, EXECTYPE FROM DATABASECHANGELOG ORDER BY ORDEREXECUTED;
```

Результат: серед записів є 004-quartz-schema-h2 зі статусом EXECUTED

<img width="1019" height="527" alt="Схема Quartz створена через Liquibase" src="https://github.com/user-attachments/assets/2771091c-01ad-4398-93ce-a548a8c1abe6" />


#### D) Ідемпотентність нагадування
Для демонстрації cron нагадування тимчасово змінено на кожні 10 секунд (`0/10 * * * * ?`), після чого `last_reminded_at` скинуто командою `UPDATE book SET last_reminded_at = NULL`. 

**Лог:**
```text
01:36:40  Reminder sent to anna: Reminder: keep reading 1984
01:36:40  Reading reminders sent: 1
01:36:50  Reading reminders sent: 0
01:37:00  Reading reminders sent: 0
```

<img width="1749" height="122" alt="Ідемпотентність ReadingReminderJob  Перший запуск знайшов книгу зі статусом READING, надіслав нагадування і записав час у last_reminded_at (sent 1)  Повторні запуски вже нічого не знаходять (sent 0)" src="https://github.com/user-attachments/assets/2c0e28ed-ca0a-4a26-88b1-ab243fc0edd2" />

#### E) Ідемпотентність звіту
Для демонстрації cron тижневого звіту тимчасово змінено на кожні 10 секунд. Після кількох запусків:

```sql
SELECT * FROM reading_report;
```

<img width="1362" height="537" alt="таблиця reading_report з одним рядком WEEKLY після багатьох запусків" src="https://github.com/user-attachments/assets/ba2c90c0-008a-43c2-a784-ad0d46e42ca1" />

