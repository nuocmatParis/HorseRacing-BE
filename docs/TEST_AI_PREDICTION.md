# Test AI Prediction

## Dữ liệu test có sẵn trong `horse_racing_mysql_schema.sql`

Sau khi chạy schema SQL, dữ liệu test đã có sẵn:

| Entity | ID | Ghi chú |
|--------|-----|---------|
| Admin | `20000000-...-0001` | username: `admin`, password: `123456` |
| Owner | `20000000-...-0002` | username: `owner1` |
| Spectator | `20000000-...-0004` | username: `spectator1` |

### 3 cặp đấu (RaceEntry):

| Lane | Horse | Rating | Wins/Races | Jockey | Exp | Wins/Races |
|------|-------|--------|------------|--------|-----|------------|
| 1 | Lightning Bolt | 95 | 8/20 (40%) | Jockey Alpha | 10y | 45/200 (22.5%) |
| 2 | Midnight Star | 82 | 5/15 (33%) | Jockey Beta | 5y | 18/80 (22.5%) |
| 3 | Golden Wind | 90 | 10/25 (40%) | Jockey Gamma | 2y | 3/25 (12%) |

### Thông tin race:
- ID: `80000000-0000-0000-0000-000000000001`
- Tên: "Opening Heat", 1600m, TURF

## Các bước test

### 1. Login lấy token
```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "123456"
}
```
Response lấy `token`.

### 2. Generate AI prediction (dự đoán top 3)
```http
POST /api/admin/races/80000000-0000-0000-0000-000000000001/ai-predictions?topN=3
Authorization: Bearer <token>
```

> `topN` là tham số admin nhập vào (VD: 3 = dự đoán % vào top 1-2-3).
> Có thể thay bằng `?topN=5` nếu muốn dự đoán top 5.

### 3. Xem kết quả (ADMIN)
```http
GET /api/admin/races/80000000-0000-0000-0000-000000000001/ai-predictions
Authorization: Bearer <token>
```

### 4. Xem kết quả (SPECTATOR)
Login với `spectator1` / `123456`, lấy token, rồi gọi:
```http
GET /api/spectator/races/80000000-0000-0000-0000-000000000001/ai-predictions
Authorization: Bearer <spectator_token>
```

## Kết quả mong đợi

### Ý nghĩa các field:
- `predictedTopN`: top N mà entry có khả năng lọt vào (VD: 3 = lọt top 3, về hạng 1, 2 hoặc 3)
- `topNProbability`: % cơ hội lọt vào top predictedTopN
- `winProbability`: % cơ hội thắng (rank 1)

### Sort:
Kết quả sort theo `topNProbability` từ **cao → thấp**

AI sẽ phân tích dựa trên:
- Lightning Bolt (Rating 95) + Jockey Alpha (45 wins) → nhiều khả năng nhất
- Golden Wind (Rating 90) + Jockey Gamma (ít kinh nghiệm) → trung bình
- Midnight Star (Rating 82) + Jockey Beta → thấp nhất

### Response mẫu:
```json
{
  "result": [
    {
      "laneNumber": 1,
      "horseName": "Lightning Bolt",
      "jockeyName": "Jockey Alpha",
      "predictedTopN": 3,
      "topNProbability": 95.00,
      "winProbability": 50.00,
      "confidenceScore": 90.00,
      "predictionReason": "Lightning Bolt has the highest rating and an experienced jockey."
    },
    {
      "laneNumber": 3,
      "horseName": "Golden Wind",
      "jockeyName": "Jockey Gamma",
      "predictedTopN": 3,
      "topNProbability": 70.00,
      "winProbability": 30.00,
      "confidenceScore": 72.00,
      "predictionReason": "Golden Wind has strong ratings but an inexperienced jockey."
    },
    {
      "laneNumber": 2,
      "horseName": "Midnight Star",
      "jockeyName": "Jockey Beta",
      "predictedTopN": 3,
      "topNProbability": 60.00,
      "winProbability": 20.00,
      "confidenceScore": 68.00,
      "predictionReason": "Midnight Star has lower ratings and moderate jockey experience."
    }
  ]
}
```

## Kiểm tra DB
```sql
SELECT entry_id, predicted_top_n, top_n_probability, win_probability, confidence_score
FROM ai_predictions
ORDER BY top_n_probability DESC;
```

## Lưu ý
- Nếu generate lần 2, code tự động xóa dữ liệu cũ và insert mới
- Nếu gặp lỗi `AI_PREDICTION_GENERATION_FAILED` → kiểm tra kết nối OpenAI/Yescale