Лабораторна робота №3: Анотації
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


--------------------------
Лабораторна робота №4: AOP
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

a. Перший випадок (успішний шлях):
Шлях виклику: POST /api/books → BookController → bookService.create(...)
Через проксі?: Так.
Результат: Є запис AUDIT ... BOOK_CREATED, працює retry.
   
b. Другий випадок (проблемний шлях):
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

--------------------------
Лабораторна робота №5: Асинхронна обробка
Тема: Асинхронна обробка (@Async, CompletableFuture, ThreadPoolTaskExecutor). Реалізовано у пакеті com.booktracker.booktracker.async.

Виконавець
AsyncConfig визначає бін ThreadPoolTaskExecutor (bookTaskExecutor) з явними обмеженнями: базовий пул 2, максимальний 4, черга 10, префікс потоків book-async-. Політика відхилення задана явно: AbortPolicy. Цей виконавець є виконавцем за замовчуванням для @Async (AsyncConfigurer), тому методи не потрапляють ні в пул Spring Boot за замовчуванням, ні в SimpleAsyncTaskExecutor. Пул вміщує 4 завдання, що виконуються, і 10 в черзі, тобто 14.

Три повільні побічні ефекти
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

Тести
a) AsyncConfigTest: потік має префікс book-async-, не default і не SimpleAsyncTaskExecutor, виконавець має явні розміри пулу й черги.
b) NotificationServiceTest: метод повертає керування одразу, завершується пізніше в потоці виконавця; збій у @Async void фіксується AsyncUncaughtExceptionHandler.
c) BookDetailsServiceTest: два виклики виконуються одночасно (CyclicBarrier на двох учасників проходить лише при паралельності) і результати об'єднані; збій одного виклику дає partial-результат; повільний виклик обривається за таймаутом.
d) ExecutorSaturationTest: виконавець із 1 потоком і без черги відхиляє зайве завдання через TaskRejectedException.
e) GlobalExceptionHandlerAsyncTest, BookControllerTest: TaskRejectedException перетворюється на 503 з Retry-After, а не на 500; після створення книги викликається сповіщення з користувачем і назвою.
f) BookCreatedNotifierTest: при переповненні сповіщення пропускається без винятку.
Запуск: mvn test

Перевірка вручну
A) Fire-and-forget: POST /api/books повернув відповідь за 124 мс, а в консолі приблизно через секунду з'явився рядок Email sent to admin-user: Book added: 1984 (thread book-async-1).
<img width="1836" height="340" alt="Демо A - повільний метод не блокує, POST took 124 ms" src="https://github.com/user-attachments/assets/526a9191-c6c4-4aab-8d55-96bc45d63105" />
B) Паралельність: GET /api/books/1/details виконався за 1033 мс (рейтинг 4.5, partial = False). Якби виклики йшли по черзі, було б понад 2000 мс.
<img width="1815" height="310" alt="Демо B - два виклики паралельно, Elapsed 1033 ms" src="https://github.com/user-attachments/assets/af9d97ad-ebd2-43a8-a945-57848bae622f" />
C) Переповнення: 10 одночасних запитів до /details дали 6 відповідей 200 і 4 відповіді 503, жодної 500.
     демонструє роботу проксі: зовнішній виклик create активує аудит і retry, тоді як внутрішній виклик this.create — ні.  
<img width="1815" height="400" alt="Демо C - переповнення → 503" src="https://github.com/user-attachments/assets/d61c139b-99d8-4461-9dfc-106f9212e0e4" />

Запуск: mvn test

5. Rate limit: перевірка
Шість запитів поспіль до POST /api/authors: перші п'ять повертають OK, шостий повертає 429. Ліміт скидається через 60 секунд.
<img width="1823" height="220" alt="вивід із 429 на шостому запиті" src="https://github.com/user-attachments/assets/b7aa4593-454d-4a93-bec8-0488dea12e6f" />
