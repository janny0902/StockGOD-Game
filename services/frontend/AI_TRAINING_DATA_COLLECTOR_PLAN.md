# AI Training Data Collector Plan (Frontend)

## Objective
Build an anonymous web game that collects high-quality buy/sell decision data for model training.

## Confirmed Product Rules
1. No login required. Anyone can play.
2. User sets nickname. Duplicate nicknames are allowed.
3. Display name format is `nickname#tag` (example: trader#2).
4. One play session contains exactly 15 problems.
5. Each problem has a 1-minute time limit.
6. Every problem starts with an automatic 5% buy at current price.
7. Users can buy/sell/hold with fixed ratio buttons and custom ratio (max 100%).
8. Reason is mandatory (reason cards and/or custom text).
9. Evaluation date selection is calendar + quick buttons: 1w, 2w, 3w, 1m.
10. Problem source date must be older than one month.
11. After submission, reveal real chart up to selected date and show result.
12. Show per-problem return and session cumulative return.

## Chart Rules
1. Bull candle: red.
2. Bear candle: blue.
3. Flat candle: gray.
4. Indicators can be added/removed from a searchable list.
5. Capture which indicators were active when the answer was submitted.

## Reason Capture Rules
1. Reason cards are provided (RSI, MACD, trend, volume, support/resistance, etc.).
2. Custom text is optional.
3. A submission is valid only when at least one reason source exists:
   - reason card selected, or
   - custom reason text provided.

## Ranking Rules
1. Recalculate leaderboard every minute.
2. Show top 100 in the main screen.
3. Add crown icons for top 3 (gold/silver/bronze).
4. If current user is outside top 100, show a condensed own-rank row under rank 30.
5. Show rank, nickname#tag, country flag, final capital, return rate, percentile.

## Privacy Rules
1. Detect country from request metadata.
2. Do not store raw IP.
3. Persist country code only.

## Ads Rules
1. Persistent banner ad placement in main game flow.
2. Card ad on every answer/result screen.
3. Card ad on final summary screen.
4. Keep placements policy-safe (no forced clicks, no deceptive UI overlap).

## Session Recovery Rules
1. Save each solved problem immediately.
2. Resume from the next unsolved problem on return.
3. Already solved problems remain stored even if user never returns.

## Frontend Flow
1. Main page
2. Leaderboard visible
3. Nickname input and duplicate tag assignment preview
4. Start game
5. Problem screen
6. Answer result screen
7. Final summary screen
8. Updated leaderboard including current user

## API Scope (initial)
1. `POST /game/sessions`: create session and nickname tag.
2. `GET /game/sessions/{id}`: resume payload.
3. `GET /game/problems/next?sessionId=`: next problem.
4. `POST /game/attempts/{attemptId}/submit`: save answer and compute result.
5. `GET /leaderboard?limit=100`: top leaderboard + own rank info.
6. `GET /meta/indicators`: indicator catalog for search.
7. `GET /meta/reason-cards`: reason card catalog.

## Delivery Phases
1. Phase 1: Core UX skeleton (main/problem/result/summary/leaderboard).
2. Phase 2: Session + problem APIs integration.
3. Phase 3: Reason cards + indicator selection tracking.
4. Phase 4: Leaderboard rendering rules and own-rank condensed row.
5. Phase 5: Localization (ko/en/ja/zh) + country flag mapping (GDP top-100 list).
6. Phase 6: Ad placements and policy QA.

## Done Criteria
1. 15-problem flow completes end-to-end.
2. Timeout auto-submit works in 60 seconds.
3. `nickname#tag` is consistently assigned and displayed.
4. Country code is stored without raw IP persistence.
5. Top-100 + own-rank-outside-100 visualization works.
6. Result and summary screens show ad placeholders and are policy-safe.
