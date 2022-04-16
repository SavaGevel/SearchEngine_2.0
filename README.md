# SearchEngine_2.0
🔍 Search engine. Web applications on SpringBoot. It gets sites from application.yml and indexing all pages from sites. 
After indexing it is able to search pages according to search query on chosen site.

There is all necessary information that helps you to run and use this web application.

Before start you should to insert some valuse to application.yaml.

![Снимок экрана 2022-04-16 в 18 58 24](https://user-images.githubusercontent.com/102177550/163682269-2fbdde57-9663-424d-972d-bbc901ed48aa.png)

Insert database, database_name, database_user and database_password. Set ddl-auto: create if you want database to create.

Also you should insert sites' urls and names. These sites will be indexed and used for searching.

![Снимок экрана 2022-04-16 в 19 00 13](https://user-images.githubusercontent.com/102177550/163682338-c08175e8-4fea-4b8e-9326-5c21a106c069.png)

Insert userAgent and referrer for correct connection to sites.

![Снимок экрана 2022-04-16 в 19 02 24](https://user-images.githubusercontent.com/102177550/163682416-5bcf24ec-2326-4576-ab0d-448cd5172f9c.png)

And insert webinterfacepath. It's a path that will use for default controller.

![Снимок экрана 2022-04-16 в 19 04 00](https://user-images.githubusercontent.com/102177550/163682465-5b4da514-8970-44bd-a258-825eb08d8909.png)

After setting application is ready to run.

Web interface:

Dashboard inset contains total statistics and detailed statisic about sites.

![Снимок экрана 2022-04-16 в 19 06 30](https://user-images.githubusercontent.com/102177550/163682604-0db4d88b-03cf-401a-a1dc-72baed2c8690.png)

Management inset contains start indexing button which starts all sites indexing.
If indexing happening stratIndexing button changes to stop indexing button which stops all sites indexing.
Also there is line for page url and button add/update which starts page indexing/reindexing.

![Снимок экрана 2022-04-16 в 19 08 33](https://user-images.githubusercontent.com/102177550/163682746-7550ca3b-e863-42e4-96a3-5272912bd3fa.png)

Search inset contains drop-down list with sites and search query line.

![Снимок экрана 2022-04-16 в 19 14 29](https://user-images.githubusercontent.com/102177550/163682908-5587b809-3b39-4f4f-90fa-4b6e3fd6600b.png)
