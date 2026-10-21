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
     демонструє роботу проксі: зовнішній виклик create активує аудит і retry, тоді як внутрішній виклик this.create — ні.  
Запуск: mvn test

4. Rate limit: перевірка
Шість запитів поспіль до POST /api/authors: перші п'ять повертають OK, шостий повертає 429. Ліміт скидається через 60 секунд.
<img width="1823" height="220" alt="вивід із 429 на шостому запиті" src="https://github.com/user-attachments/assets/b7aa4593-454d-4a93-bec8-0488dea12e6f" />
