*     |--src #Директория источника всего проект      
*         |--main
               |--java
                   |--org.example.thoughts 
*                    |--controller #Контроллеры сервисов
                        |--DownloadController #Контроллер команд "мысль" и "мысля"
                        |--UploadController #Контроллер команды "цитата"
*                    |--service #Сервисы проекта
                        |--VkDownloadService #Сервис команд "мысль" и "мысля"
                        |--VkUploadService #Сервис команды "цитата"
*                    |--ThoughtsServiceApplication #Основное приложение, запускающее сервисы
*            |--resources 
                |--application.properties #Все доступы и порты. Внимание!!! Используется токен текущего председателя ССт ИТиАБД
    
          |--test #Директория для написания тестов
*     |--.gitignore
*     |--Docker
*     |--pom.xml #Maven файл, содержащий всю информацию об используемых фреймворках/библиотек и их версиях
*     |--README.md
