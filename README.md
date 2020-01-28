[JIRA] | [CI]

# N2O Cloud Report

Микросервис формирования отчетов на основе JasperReports.<br/>
Поддерживаемые форматы отчетов: **pdf, xml, csv, xls, xlsx, docx, odt, ods.**

# Инструкции

## Настройка

1. **Настройка библиотеки JasperReports.**<br/>
    http://jasperreports.sourceforge.net/config.reference.html
    
    Свойства конфигурации могут быть прописаны как в *jasperreports.properties*,<br/> так и указаны в конкретном шаблоне: 

    ```
    <property name="net.sf.jasperreports.export.pdf.encrypted" value="true"/>
    ```
 
2. **Настройка отчета.**<br/>
*parameters.properties* - зависимые от стенда(и другие) параметры отчета.<br/> 

    При совпадении имен значения, передаваемые в queryParameters, более приоритетны, чем parameters.properties.

    Значение *fileStorage.root* внутри шаблона отчета можно использовать, добавив проперти *fsRoot*

       
## Использование
1. Создать шаблон отчета (.jrxml) с помощью Jaspersoft Studio или других средств.
2. Результат скомпилировать и сохранить в файловое хранилище , выполнив POST запрос (на вход .jrxml)
Примеры шаблонов:<br/>
[accrCommand.jrxml](accrCommand.jrxml) <br/>
[command.jrxml](command.jrxml) - сабрепорт, используемый в accrCommand.jrxml<br/>
3. Сгенерировать отчет, выполнив GET запрос<br/> 
Пример запроса:<br/>
http://localhost:8080/api/report/accrCommand.pdf?status=ACCREDITED


[JIRA]: https://jira.i-novus.ru/projects/REPENG
[CI]: https://ci.i-novus.ru/view/n2o-components/job/report