# Подробная логика работы игры

## 1. Логика добавления и удаления полос (линий)

### Где происходит: `GameView.kt`

### 1.1. Определение изменения количества полос

**Место в коде:** `GameView.kt`, метод `update()`, строки **238-262**

```kotlin
val timeSinceLastAdd = System.currentTimeMillis() - lastLaneAddTime
if (timedGameMode && timeSinceLastAdd > 15000) {
    if (!isAnimatingLaneChange) {
        if (laneCount == 7) {
            increasingLanes = false  // Достигли максимума, начинаем уменьшать
        } else if (laneCount == 3) {
            increasingLanes = true   // Достигли минимума, начинаем увеличивать
        }
        oldLaneCount = laneCount
        if (increasingLanes) {
            laneCount++              // Увеличиваем количество полос
        } else {
            laneCount--              // Уменьшаем количество полос
            speedBoost += 0.5f       // При уменьшении увеличиваем скорость препятствий
        }
        if (isRunning) {
            score++                  // Начисляем очко за изменение полос
        }
        isAnimatingLaneChange = true
        laneChangeStartTs = System.currentTimeMillis()
        lastLaneAddTime = System.currentTimeMillis()
    }
} else if (timedGameMode && timeSinceLastAdd > 11000) {
    isSpawningPaused = true  // За 4 секунды до изменения останавливаем спавн
}
```

**Как работает:**
1. Проверяется, прошло ли 15 секунд с последнего изменения (`timeSinceLastAdd > 15000`)
2. Если да и анимация не идет:
   - Проверяется текущее количество полос
   - Если 7 → начинаем уменьшать (`increasingLanes = false`)
   - Если 3 → начинаем увеличивать (`increasingLanes = true`)
   - Изменяется `laneCount` (++ или --)
   - Запускается анимация (`isAnimatingLaneChange = true`)
3. За 4 секунды до изменения (11 секунд) останавливается спавн препятствий

### 1.2. Вычисление позиций полос

**Место в коде:** `GameView.kt`, метод `calculateLanePositions()`, строки **645-651**

```kotlin
private fun calculateLanePositions(w: Int, count: Int): List<Float> {
    return when (count) {
        2 -> listOf(w * 0.33f, w * 0.66f)
        3 -> listOf(w * 0.25f, w * 0.5f, w * 0.75f)
        else -> (0 until count).map { i -> (i + 0.5f) * w / count }
    }
}
```

**Как работает:**
- Принимает ширину экрана `w` и количество полос `count`
- Для 2 полос: 33% и 66% ширины экрана
- Для 3 полос: 25%, 50% и 75% ширины экрана
- Для остальных: равномерно распределяет полосы по экрану
  - Формула: `(i + 0.5) * width / count`
  - Пример для 4 полос на экране 1080px:
    - Полоса 0: (0 + 0.5) * 1080 / 4 = 135px
    - Полоса 1: (1 + 0.5) * 1080 / 4 = 405px
    - Полоса 2: (2 + 0.5) * 1080 / 4 = 675px
    - Полоса 3: (3 + 0.5) * 1080 / 4 = 945px

### 1.3. Обновление позиций полос

**Место в коде:** `GameView.kt`, метод `updateLanePositions()`, строки **641-643**

```kotlin
private fun updateLanePositions(w: Int) {
    lanePositions = calculateLanePositions(w, laneCount)
}
```

**Как работает:**
- Вызывается при изменении размера экрана или количества полос
- Пересчитывает позиции всех полос
- Обновляет `lanePositions` - список позиций X для каждой полосы

### 1.4. Анимация изменения полос

**Место в коде:** `GameView.kt`, метод `onDraw()`, строки **426-493**

```kotlin
if (isAnimatingLaneChange && oldLaneCount > 0) {
    val elapsedTime = System.currentTimeMillis() - laneChangeStartTs
    var progress = (elapsedTime.toFloat() / laneChangeAnimationDuration).coerceAtMost(1f)
    // Apply ease-in-out interpolation
    progress = 0.5f - 0.5f * Math.cos(progress * Math.PI).toFloat()

    val M = laneCount      // Новое количество полос
    val N = oldLaneCount   // Старое количество полос

    val newLanePos = calculateLanePositions(width, M)
    val oldLanePos = calculateLanePositions(width, N)
    
    // Интерполяция позиций игрока
    val interpolatedLanePos = newLanePos.map { newPos ->
        // Находим ближайшую старую позицию
        var closestOldPos = oldLanePos[0]
        var minDist = abs(oldLanePos[0] - newPos)
        for (i in 1 until oldLanePos.size) {
            val dist = abs(oldLanePos[i] - newPos)
            if (dist < minDist) {
                minDist = dist
                closestOldPos = oldLanePos[i]
            }
        }
        // Плавный переход от старой к новой позиции
        closestOldPos + (newPos - closestOldPos) * progress
    }
    player.updateLanePositions(interpolatedLanePos)

    // Анимация линий между полосами
    val oldLines = if (N > 1) {
        (0 until N - 1).map { j ->
            (oldLanePos[j] + oldLanePos[j + 1]) / 2
        }
    } else {
        emptyList()
    }
    val newLines = if (M > 1) {
        (0 until M - 1).map { j ->
            (newLanePos[j] + newLanePos[j + 1]) / 2
        }
    } else {
        emptyList()
    }
    
    // Рисуем линии с плавной анимацией
    newLines.forEachIndexed { index, newLine ->
        val closestOldLine = if (oldLines.isNotEmpty()) {
            // Находим ближайшую старую линию
            var closest = oldLines[0]
            var minDist = abs(oldLines[0] - newLine)
            for (i in 1 until oldLines.size) {
                val dist = abs(oldLines[i] - newLine)
                if (dist < minDist) {
                    minDist = dist
                    closest = oldLines[i]
                }
            }
            closest
        } else {
            width / 2f
        }
        val currentPos = closestOldLine + (newLine - closestOldLine) * progress
        canvas.drawLine(currentPos, 0f, currentPos, height.toFloat(), lanePaint)
    }

    // Когда анимация завершена
    if (progress == 1f) {
        isAnimatingLaneChange = false
        updateLanePositions(width)
        player.updateLanePositions(lanePositions)
        // Обновляем позиции для препятствий, которые меняют полосы
        obstacles.forEach { obstacle ->
            if (obstacle.type == ObstacleType.LANE_CHANGER) {
                obstacle.updateLanePositions(lanePositions)
            }
        }
        lastSpawnEmptyLanes = setOf()
        isSpawningPaused = false
    }
}
```

**Как работает анимация:**
1. Вычисляется прогресс анимации (0.0 до 1.0) за 1500ms
2. Применяется ease-in-out через косинус для плавности
3. Вычисляются позиции старых и новых полос
4. Для каждой новой полосы находится ближайшая старая
5. Плавно интерполируется позиция от старой к новой
6. Игрок и линии плавно перемещаются
7. Когда `progress == 1f`, анимация завершается и обновляются все позиции

**Визуальный эффект:**
- Полосы плавно "раздвигаются" или "сдвигаются"
- Игрок плавно перемещается на новую позицию
- Линии между полосами плавно перемещаются

### 1.5. Отрисовка полос (без анимации)

**Место в коде:** `GameView.kt`, метод `onDraw()`, строки **494-498**

```kotlin
} else {
    // Обычная отрисовка полос (когда нет анимации)
    for (i in 0 until lanePositions.size - 1) {
        val left = lanePositions[i] + (lanePositions[i+1] - lanePositions[i]) / 2
        canvas.drawLine(left, 0f, left, height.toFloat(), lanePaint)
    }
}
```

**Как работает:**
- Рисует линии между полосами
- Линия находится посередине между двумя соседними полосами
- Количество линий = количество полос - 1

---

## 2. Логика добавления препятствий (спавн)

### Где происходит: `GameView.kt`

### 2.1. Вызов спавна

**Место в коде:** `GameView.kt`, метод `update()`, строки **210-214**

```kotlin
frameCount++
// Не спавним новые препятствия во время замедления, чтобы они не наслаивались
if (!slowdownActive && frameCount % spawnFrequency == 0 && !isSpawningPaused && !isAnimatingLaneChange) {
    spawnObstacle()
}
```

**Как работает:**
1. Увеличивается счетчик кадров `frameCount`
2. Проверяются условия для спавна:
   - Нет активного замедления (`!slowdownActive`)
   - Прошло нужное количество кадров (`frameCount % spawnFrequency == 0`)
   - Спавн не на паузе (`!isSpawningPaused`)
   - Нет анимации изменения полос (`!isAnimatingLaneChange`)
3. Если все условия выполнены → вызывается `spawnObstacle()`

**Частота спавна:**
- `spawnFrequency` - количество кадров между спавнами
- Меньше значение = чаще спавн
- По умолчанию: 33 кадра (при 60 FPS = ~0.55 секунды)

### 2.2. Основная логика спавна

**Место в коде:** `GameView.kt`, метод `spawnObstacle()`, строки **359-414**

#### Шаг 1: Определение количества препятствий

```kotlin
val lanesToSpawnIn = lastSpawnEmptyLanes.toMutableSet()
val minObstacles = lastSpawnEmptyLanes.size.coerceAtLeast(1)
val maxObstacles = (laneCount - 1).coerceAtLeast(minObstacles)

val numberOfObstacles = if (minObstacles >= maxObstacles) {
    maxObstacles
} else {
    (minObstacles..maxObstacles).random()
}
```

**Как работает:**
- `lastSpawnEmptyLanes` - полосы, которые были пустыми в предыдущем спавне
- Минимум: количество пустых полос (но не меньше 1)
- Максимум: количество полос - 1 (всегда оставляем хотя бы одну пустую)
- Случайно выбирается количество между минимумом и максимумом

**Пример:**
- 3 полосы, в прошлый раз были пустыми полосы 0 и 2
- minObstacles = 2, maxObstacles = 2
- numberOfObstacles = 2

#### Шаг 2: Выбор полос для спавна

```kotlin
val remainingNeeded = numberOfObstacles - lanesToSpawnIn.size
if (remainingNeeded > 0) {
    val availableForRandom = (0 until laneCount).toSet() - lanesToSpawnIn
    lanesToSpawnIn.addAll(availableForRandom.shuffled().take(remainingNeeded))
}
```

**Как работает:**
1. Вычисляется, сколько еще препятствий нужно
2. Берется список доступных полос (все полосы минус уже выбранные)
3. Полосы перемешиваются и берутся нужное количество
4. Добавляются к `lanesToSpawnIn`

**Гарантия:**
- Не все полосы будут заняты одновременно
- Всегда есть хотя бы одна пустая полоса

#### Шаг 3: Определение типа препятствий

```kotlin
// Проверяем, есть ли разблокированные специальные типы препятствий
val unlockedTypes = getUnlockedObstacleTypes()
val hasUnlockedSpecialTypes = unlockedTypes.any { it != ObstacleType.NORMAL }

// Определяем, будет ли одно специальное препятствие в этом спавне
val hasSpecialObstacle = hasUnlockedSpecialTypes && lanesToSpawnIn.size > 1 && (0..100).random() < 30
// Выбираем случайную позицию для специального препятствия
val specialObstacleIndex = if (hasSpecialObstacle) {
    (0 until lanesToSpawnIn.size).random()
} else {
    -1 // Нет специального препятствия
}
```

**Как работает:**
1. Проверяется, разблокированы ли специальные типы (score >= 5)
2. Если да и спавнится больше 1 препятствия:
   - С вероятностью 30% будет одно специальное препятствие
   - Выбирается случайная позиция для него
3. Остальные препятствия всегда NORMAL

#### Шаг 4: Создание препятствий

```kotlin
lanesToSpawnIn.forEachIndexed { index, lane ->
    if (lane < lanePositions.size) {
        val x = lanePositions[lane]
        // Выбираем тип препятствия: одно специальное, остальные NORMAL
        val type = if (index == specialObstacleIndex) {
            selectRandomObstacleType()
        } else {
            ObstacleType.NORMAL
        }
        val obstacle = Obstacle(x, 0f, type, speedBoost,
            if (type == ObstacleType.LANE_CHANGER) lanePositions else null)

        if (slowdownActive) {
            obstacle.slowDown()
        } else if (speedupActive) {
            obstacle.speedUp()
        }
        obstacles.add(obstacle)
    }
}

lastSpawnEmptyLanes = (0 until laneCount).toSet() - lanesToSpawnIn
```

**Как работает:**
1. Для каждой выбранной полосы:
   - Берется позиция X из `lanePositions[lane]`
   - Определяется тип (специальное или NORMAL)
   - Создается Obstacle в позиции (x, 0) - сверху экрана
   - Применяются эффекты замедления/ускорения
   - Добавляется в список препятствий
2. Сохраняются пустые полосы для следующего спавна

### 2.3. Выбор типа препятствия

**Место в коде:** `GameView.kt`, метод `selectRandomObstacleType()`, строки **307-357**

```kotlin
private fun selectRandomObstacleType(): ObstacleType {
    val unlockedTypes = getUnlockedObstacleTypes()
    
    // Если разблокирован только NORMAL, возвращаем его
    if (unlockedTypes.size == 1) {
        return ObstacleType.NORMAL
    }
    
    // Вероятности появления разных типов препятствий (только разблокированных)
    val rand = (1..100).random()
    val normalWeight = 40
    val fastWeight = 20
    val slowWeight = 15
    val smallWeight = 10
    val bigWeight = 7
    val laneChangerWeight = 8
    
    // Вычисляем общий вес доступных типов
    var currentWeight = 0
    val totalWeight = unlockedTypes.sumOf { type ->
        when (type) {
            ObstacleType.NORMAL -> normalWeight
            ObstacleType.FAST -> fastWeight
            ObstacleType.SLOW -> slowWeight
            ObstacleType.SMALL -> smallWeight
            ObstacleType.BIG -> bigWeight
            ObstacleType.LANE_CHANGER -> laneChangerWeight
        }
    }
    
    // Нормализуем случайное число относительно доступных типов
    val normalizedRand = (rand * totalWeight) / 100
    
    // Выбираем тип по весам
    currentWeight = 0
    for (type in unlockedTypes) {
        val typeWeight = when (type) {
            ObstacleType.NORMAL -> normalWeight
            ObstacleType.FAST -> fastWeight
            ObstacleType.SLOW -> slowWeight
            ObstacleType.SMALL -> smallWeight
            ObstacleType.BIG -> bigWeight
            ObstacleType.LANE_CHANGER -> laneChangerWeight
        }
        currentWeight += typeWeight
        if (normalizedRand <= currentWeight) {
            return type
        }
    }
    
    // Fallback на первый доступный тип
    return unlockedTypes.first()
}
```

**Как работает:**
1. Получает список разблокированных типов
2. Если только NORMAL → возвращает NORMAL
3. Иначе использует систему весов:
   - NORMAL: 40%
   - FAST: 20%
   - SLOW: 15%
   - SMALL: 10%
   - BIG: 7%
   - LANE_CHANGER: 8%
4. Выбирает тип случайно, но с учетом весов
5. Учитываются только разблокированные типы

### 2.4. Разблокировка типов препятствий

**Место в коде:** `GameView.kt`, метод `getUnlockedObstacleTypes()`, строки **293-305**

```kotlin
private fun getUnlockedObstacleTypes(): List<ObstacleType> {
    val unlocked = mutableListOf(ObstacleType.NORMAL) // Всегда доступны обычные
    
    // Используем if для накопления - каждый тип разблокируется и остается доступным
    if (score >= 5) unlocked.add(ObstacleType.FAST)      // После 5 очков
    if (score >= 10) unlocked.add(ObstacleType.SLOW)      // После 10 очков
    if (score >= 15) unlocked.add(ObstacleType.SMALL)     // После 15 очков
    if (score >= 20) unlocked.add(ObstacleType.BIG)       // После 20 очков
    if (score >= 25) unlocked.add(ObstacleType.LANE_CHANGER) // После 25 очков
    
    return unlocked
}
```

**Как работает:**
- Всегда доступен NORMAL
- По мере набора очков добавляются новые типы
- Разблокированные типы остаются доступными до конца игры

---

## Схема работы спавна препятствий

```
update() вызывается каждый кадр
    ↓
frameCount++ (увеличиваем счетчик)
    ↓
Проверка условий:
- !slowdownActive?
- frameCount % spawnFrequency == 0?
- !isSpawningPaused?
- !isAnimatingLaneChange?
    ↓ (если все true)
spawnObstacle()
    ↓
1. Определяем количество препятствий
2. Выбираем полосы для спавна
3. Определяем тип препятствий
4. Создаем Obstacle для каждой полосы
5. Добавляем в список obstacles
6. Сохраняем пустые полосы для следующего спавна
```

---

## Схема работы изменения полос

```
update() вызывается каждый кадр
    ↓
Проверка: timedGameMode && timeSinceLastAdd > 15000?
    ↓ (если true)
Определяем направление изменения:
- laneCount == 7 → increasingLanes = false (уменьшаем)
- laneCount == 3 → increasingLanes = true (увеличиваем)
    ↓
Изменяем laneCount (++ или --)
    ↓
Запускаем анимацию: isAnimatingLaneChange = true
    ↓
onDraw() каждый кадр:
- Вычисляем progress (0.0 → 1.0 за 1500ms)
- Интерполируем позиции полос
- Рисуем линии с анимацией
    ↓
Когда progress == 1f:
- Завершаем анимацию
- Обновляем все позиции
- Возобновляем спавн
```

---

## Важные детали

### Почему не все полосы заняты?
- `maxObstacles = (laneCount - 1)` - всегда на 1 меньше количества полос
- Это гарантирует, что всегда есть хотя бы одна пустая полоса для маневра

### Почему спавн останавливается перед изменением полос?
- За 4 секунды до изменения (`timeSinceLastAdd > 11000`)
- Это предотвращает появление препятствий во время анимации
- После завершения анимации спавн возобновляется

### Почему во время замедления не спавнятся препятствия?
- Чтобы препятствия не наслаивались друг на друга
- Замедленные препятствия движутся медленнее, новые бы их догнали

### Как работает система весов?
- Каждый тип имеет "вес" (вероятность)
- Сумма весов = 100
- Случайное число от 1 до 100 попадает в диапазон одного из типов
- Тип с большим весом имеет больше шансов появиться

