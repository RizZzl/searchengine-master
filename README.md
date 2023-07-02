__<h1>Разработка локального поискового движка по сайту</h1>__
Этот проект представляет собой локальный поисковый движок для индексации и поиска по веб-страницам сайта. Проект включает в себя следующие компоненты:

* <span style="color:blue">**IndexingService**</span>: сервис для индексации веб-страниц сайта. Он обрабатывает список заданных сайтов, индексирует каждую страницу и сохраняет результаты в базе данных. Сервис поддерживает возможность остановки индексации по запросу пользователя.

* <span style="color:blue">**LemmaService**</span>: сервис для обработки текста веб-страницы и подсчета лемм (словоформ). Он извлекает текст из HTML-контента страницы, удаляет HTML-теги, считает леммы слов и подсчитывает их частоту. Результаты сохраняются для дальнейшего использования при индексации.

* <span style="color:blue">**SearchService**</span>: сервис для выполнения поисковых запросов. Он принимает запрос пользователя, ищет соответствующие веб-страницы в индексе и рассчитывает их релевантность на основе совпадения лемм. Результаты поиска возвращаются пользователю.

__<h2>Стэк используемых технологий</h2>__
* Язык программирования: <span style="color:red">**Java**</span>
* Фреймворк: <span style="color:red">**Spring Boot**</span>
* Система управления базами данных: <span style="color:red">**MySQL**</span>
* Библиотека для парсинга HTML: <span style="color:red">**Jsoup**</span>
* Библиотека для морфологического анализа русского языка: <span style="color:red">**Lucene Morphology**</span>

__<h2>Инструкция по локальному запуску проекта</h2>__

1. Установите Java Development Kit (JDK) версии 8 или выше, если его еще нет.
2. Установите MySQL и создайте базу данных для проекта.
3. Склонируйте репозиторий проекта на свою локальную машину.
4. Откройте файл application.properties в каталоге src/main/resources.
5. Внесите необходимые изменения в файл application.properties, чтобы настроить подключение к базе данных MySQL.
6. Соберите проект и запустите его с помощью системы сборки Maven или в вашей интегрированной среде разработки (IDE).
7. Приложение должно быть доступно по адресу http://localhost:8080.

Дополнительно, перед запуском проекта, рекомендуется убедиться, что все зависимости, указанные в файле pom.xml, были успешно загружены и установлены.