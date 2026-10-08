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
