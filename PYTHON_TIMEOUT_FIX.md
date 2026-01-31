# Исправление timeout для Python бота (Test_bot)

## Проблема
Python выбрасывает `Request timeout` при вызове Java‑сервиса: Java успевает принять запрос и начать обработку, но Python обрывает соединение раньше, чем Java успевает получить фото из VK API и загрузить его.

## Решение

### 1. Файл `apps/VK_service.py`

**Текущий таймаут:** 60 секунд — может не хватать при медленном VK API и больших фото.

**Нужно изменить класс `VkPhotoService`:**

```python
class VkPhotoService:
        self.base_url = Config.JAVA_SERVICE_URL.rstrip('/')
        # Java read-timeout 180 сек, нужен запас
        self.timeout = aiohttp.ClientTimeout(
            total=210,      # ~3.5 мин (Java: до 180 сек на загрузку с VK CDN)
            connect=15,
            sock_read=200
        )
```

### 2. Файл `docker-compose.yaml`

**Ошибка:** сервисы используют сеть `network`, а определена `app-network`.

Заменить:
```yaml
networks:
  - network
```

на:
```yaml
networks:
  - app-network
```

Или переименовать определение сети:
```yaml
networks:
  network:
    driver: bridge
```

### 3. Проверка URL

В `docker-compose` указано `JAVA_SERVICE_URL=http://java-service:8080/api`.

В `VK_service.py` используется `url = f"{self.base_url}/api/random-photo"`.

Если `base_url` = `http://java-service:8080/api`, то итоговый URL будет `http://java-service:8080/api/api/random-photo` — лишний `/api`.

**Варианты:**
- Либо `JAVA_SERVICE_URL=http://java-service:8080`
- Либо в коде: `url = f"{self.base_url}/random-photo"` (без `/api`)

Судя по логам (`Requesting photo from: http://java-service:8080/api/random-photo`), у вас уже используется правильный URL — проверьте конфигурацию.

## Итог изменений

| Компонент     | Изменение                                              |
|---------------|--------------------------------------------------------|
| Java          | RestTemplate с connect=10s, read=90s                   |
| Java          | Оптимизация: sleep 50ms вместо 100ms в getPhotoByYear  |
| Python        | Таймаут 120s, connect 15s, sock_read 110s              |
| docker-compose| Согласовать имя сети                                   |
