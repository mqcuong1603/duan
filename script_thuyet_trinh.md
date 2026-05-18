# Kịch bản thuyết trình — Đồ án Tốt nghiệp

**Đề tài:** *Object-Oriented Solutions to the Problem of Mining Top-K Maximal Frequent Itemsets from Uncertain Databases*

**Sinh viên:** Mã Quốc Cường (522I0001) & Nguyễn Cao Phi (521V0006)
**GVHD:** Prof. Nguyễn Chí Thiện
**Tổng thời gian dự kiến:** ~18–20 phút trình bày + Q&A
**Số slide:** 23

**Gợi ý phân chia:** Cường trình bày slide 1–12, Phi trình bày slide 13–23 (có thể đổi tuỳ ý).

---

## ⚙️ Mẹo chung trước khi bắt đầu

- Hít sâu một nhịp trước khi mở miệng nói câu đầu tiên — đừng vội.
- Nói chậm hơn bình thường khoảng 15–20%; nhấn nhịp ở những con số và thuật ngữ quan trọng.
- Khi gặp công thức hoặc bảng số: chỉ tay vào màn hình để dẫn mắt khán giả.
- Khi chuyển slide, thêm những cụm nối ngắn như *"Tiếp theo…"*, *"Bây giờ mình xem…"*, *"Vậy thì…"* — giúp khán giả bắt kịp nhịp.
- Nếu lỡ vấp, đừng xin lỗi — chỉ cần dừng nửa nhịp rồi nói tiếp tự nhiên.
- Giữ giao tiếp bằng mắt với hội đồng, đừng chỉ nhìn slide.

---

## Slide 1 — Bìa (≈40 giây)

**[Cường mở đầu]**

> Kính thưa thầy cô trong hội đồng, kính thưa thầy Nguyễn Chí Thiện, và xin chào tất cả các bạn.
>
> Em là Mã Quốc Cường, MSSV 522I0001, cùng với bạn em là Nguyễn Cao Phi, MSSV 521V0006. Hôm nay nhóm em xin được trình bày đồ án tốt nghiệp với chủ đề:
>
> ***"Giải pháp Hướng đối tượng cho Bài toán Khai phá Top-K Tập phổ biến Tối đại từ Cơ sở dữ liệu Không chắc chắn"*** — dưới sự hướng dẫn của thầy Nguyễn Chí Thiện.
>
> Bài thuyết trình của tụi em sẽ kéo dài khoảng 18–20 phút. Sau đó tụi em rất mong nhận được nhận xét và câu hỏi từ hội đồng.

**Chuyển slide:** *"Trước tiên, mời thầy cô và các bạn xem qua bố cục bài trình bày."*

---

## Slide 2 — Outline (≈30 giây)

> Bài thuyết trình của nhóm em gồm 7 phần chính:
>
> 1. **Động lực** — vì sao tụi em chọn bài toán này.
> 2. **Phát biểu bài toán** — định nghĩa rõ đầu vào và đầu ra.
> 3. **Background** — một vài kiến thức nền cần thiết.
> 4. **Hai thuật toán** UGenMax và UFPMax.
> 5. **Chế độ Top-K** — đóng góp chính của đồ án.
> 6. **Thực nghiệm** trên ba bộ dữ liệu chuẩn.
> 7. **Kết luận** và hướng phát triển.

**Chuyển slide:** *"Bây giờ em xin bắt đầu với phần Động lực."*

---

## Slide 3 — Motivation: Uncertain Database (≈50 giây)

> Trước khi đi vào bài toán, mình cần làm rõ một khái niệm: **cơ sở dữ liệu không chắc chắn**, hay còn gọi là cơ sở dữ liệu xác suất.
>
> Khác với CSDL thông thường — nơi dữ liệu là **chắc chắn**: có hoặc không có — trong CSDL không chắc chắn, **mỗi item đi kèm một xác suất tồn tại**, cho biết item đó có thật sự xuất hiện hay không.
>
> Loại dữ liệu này xuất hiện rất phổ biến trong thực tế. Hai ví dụ tiêu biểu các bạn thấy ở đây:
>
> - **RFID** — đầu đọc thẻ vô tuyến. Do nhiễu sóng, đầu đọc đôi khi không chắc 100% nó đã đọc đúng thẻ nào, nên mỗi lần đọc thường đi kèm xác suất tin cậy.
> - **Mạng cảm biến** — sensor đo nhiệt độ, độ ẩm, lưu lượng giao thông… đều có sai số, nên giá trị nhận về cũng mang tính xác suất.
>
> Ngoài ra còn có dữ liệu y tế tích hợp, dữ liệu tài chính có nhiễu — tất cả đều là CSDL không chắc chắn.

**Chuyển slide:** *"Vậy ở góc độ dữ liệu giao dịch, một CSDL không chắc chắn cụ thể trông như thế nào?"*

---

## Slide 4 — Motivation: Cần thuật toán mới (≈45 giây)

> Bên trái slide là một ví dụ thực tế khác: hồ sơ y tế tích hợp từ nhiều nguồn — mỗi chẩn đoán có thể đi kèm một độ tin cậy khác nhau.
>
> Bên phải là bộ dữ liệu mẫu **contextUncertain** mà nhóm em dùng trong báo cáo để minh hoạ. Các bạn để ý: mỗi item đi kèm một con số trong ngoặc — đó chính là **xác suất tồn tại**. Ví dụ ở dòng t₁, item 1 có xác suất 0.8, tức là 80% chắc item này thật sự xuất hiện trong giao dịch đó.
>
> Vấn đề là: **các thuật toán khai phá cổ điển — như Apriori hay FP-Growth — đều giả định dữ liệu là tuyệt đối** (có hoặc không). Khi áp lên dữ liệu xác suất, chúng cho kết quả không có ý nghĩa thống kê.
>
> Đó là lý do nhóm em phải xây dựng thuật toán riêng cho loại dữ liệu này.

**Chuyển slide:** *"Bây giờ tụi em phát biểu chính thức bài toán."*

---

## Slide 5 — Problem Definition: Input/Output (≈30 giây)

> Bài toán nhóm em giải quyết được phát biểu rất gọn:
>
> - **Đầu vào:** một cơ sở dữ liệu giao dịch không chắc chắn — mỗi item đi kèm xác suất tồn tại.
> - **Đầu ra:** **K tập phổ biến tối đại** có tần suất kỳ vọng cao nhất, **và đặc biệt là người dùng không cần chỉ định ngưỡng minsup**.
>
> Điểm "không cần chọn minsup" là điểm mấu chốt — tụi em sẽ giải thích vì sao nó quan trọng ở phần Top-K phía sau.

**Chuyển slide:** *"Để hiểu đầu ra, mình cần làm rõ hai khái niệm: tập phổ biến, và tập phổ biến tối đại."*

---

## Slide 6 — Tập Phổ biến (Frequent Itemset) (≈45 giây)

> Trước hết, **tập phổ biến là gì?** Nói đơn giản: một nhóm các mặt hàng thường xuất hiện cùng nhau trong nhiều giao dịch.
>
> Ví dụ ở đây với ngưỡng minsup = 2:
>
> - **{Bread}** xuất hiện ở T1, T2, T4 → 3 lần → phổ biến.
> - **{Diapers, Beer}** xuất hiện ở T2, T3, T4 → 3 lần → phổ biến.
> - **{Bread, Milk}** xuất hiện ở T1, T4 → 2 lần → vừa đủ phổ biến.
> - **{Eggs}** chỉ ở T2 → 1 lần → **không** phổ biến.
>
> Quy tắc: nếu một tập xuất hiện ở ít nhất `minsup` giao dịch thì gọi là phổ biến.

**Chuyển slide:** *"Tuy nhiên, danh sách các tập phổ biến thường rất dài. Đó là lý do mình quan tâm đến tập 'tối đại'."*

---

## Slide 7 — Tập Phổ biến Tối đại (MFI) (≈50 giây)

> **Tập phổ biến tối đại** — viết tắt là **MFI** — là tập phổ biến mà **không thể mở rộng thêm**: không tồn tại bất kỳ tập lớn hơn nào chứa nó mà vẫn phổ biến.
>
> Một tập X là MFI khi thoả hai điều kiện:
>
> 1. **Bản thân nó phổ biến** — sup(X) ≥ minsup.
> 2. **Không có tập cha nào còn phổ biến** — với mọi item y ∉ X, sup(X ∪ {y}) < minsup.
>
> Với ví dụ ở đây, minsup = 3, có 3 MFI: **{Diapers, Beer}**, **{Bread}**, và **{Milk}**.
>
> **Tại sao quan tâm đến MFI?** Vì MFI là **bộ nén** — biết được các MFI là biết được tất cả các tập phổ biến (vì mọi tập phổ biến đều là tập con của ít nhất một MFI). Thay vì hàng nghìn kết quả, ta có một danh sách ngắn gọn hơn rất nhiều.

**Chuyển slide:** *"Bây giờ vào phần Background — vì sao bài toán này khó với dữ liệu không chắc chắn?"*

---

## Slide 8 — Possible World Semantics (≈60 giây)

> Khi dữ liệu không chắc chắn, **mỗi xác suất chia thế giới thành nhiều "thế giới có thể"** — gọi là *possible worlds*.
>
> Ví dụ: nếu CSDL có 3 giao dịch, mỗi giao dịch có thể tồn tại hoặc không, thì có **2³ = 8 thế giới có thể**.
>
> Slide minh hoạ hai thế giới:
> - **Thế giới 1**: T1 và T3 tồn tại, T2 không tồn tại. Xác suất xảy ra = 0.5 × 0.4 × 0.7 = **0.14**.
> - **Thế giới 2**: chỉ T2 tồn tại. Xác suất = 0.5 × 0.6 × 0.3 = **0.09**.
>
> **Vấn đề cốt lõi:** để tính support thật của một tập, lý thuyết yêu cầu duyệt qua tất cả 2ⁿ thế giới — **không khả thi** với dữ liệu thực tế. Thử tưởng tượng với 80,000 giao dịch — không một máy tính nào tính nổi.
>
> Đó là lý do mình phải dùng **mô hình Expected Support** (Tần suất Kỳ vọng) — đơn giản hơn nhưng vẫn có ý nghĩa toán học rõ ràng.

**Chuyển slide:** *"Trước khi vào mô hình Expected Support, em giới thiệu nhanh hai thuật toán nhóm em đề xuất."*

---

## Slide 9 — Algorithms Overview (≈40 giây)

> Nhóm em đề xuất hai thuật toán:
>
> - **UGenMax** — chiến lược **bottom-up** (từ dưới lên): backtracking theo chiều sâu trên **weighted tidset** — danh sách giao dịch có trọng số xác suất.
> - **UFPMax** — chiến lược **top-down** (từ trên xuống): pattern growth trên FP-tree, dùng kỹ thuật chiếu cơ sở dữ liệu có điều kiện.
>
> Cả hai đều **kiểm tra tính tối đại ngay trong quá trình tìm kiếm** (inline) — không cần hậu xử lý.
>
> Hai thuật toán này được nhóm em **thích ứng từ GenMax (Gouda & Zaki, 2005)** và **FPMax (Grahne & Zhu, 2003)** cho dữ liệu không chắc chắn.

**Chuyển slide:** *"Bây giờ vào chi tiết: mô hình support trên dữ liệu xác suất."*

---

## Slide 10 — Support Model (≈45 giây)

> Khi item có xác suất, **support không còn là một con số cố định nữa**, mà trở thành một **biến ngẫu nhiên**.
>
> Ví dụ ở đây với tập {a, b}, ba giao dịch T1, T2, T3 với xác suất tương ứng. Câu hỏi đặt ra: *"Tập {a, b} có phổ biến không?"*
>
> Vấn đề là: số lần thật sự xuất hiện của {a, b} có thể là **0, 1, hoặc 2** — mỗi giá trị có một xác suất riêng. Vậy mình lấy giá trị nào để so với minsup?
>
> Câu trả lời ở slide tiếp theo: **dùng giá trị trung bình** — chính là Expected Support.

**Chuyển slide:** *"Đó cũng là mô hình mà nhóm em chọn."*

---

## Slide 11 — Expected Support (≈55 giây)

> **Expected Support** trả lời câu hỏi: *"Trung bình, tập này xuất hiện bao nhiêu lần?"*
>
> Công thức rất đơn giản: với mỗi giao dịch, lấy **tích xác suất** của các item trong tập, rồi **cộng tất cả lại**.
>
> Ví dụ tính E[sup({a, b})]:
> - Ở T1: 0.9 × 0.8 = **0.72**
> - Ở T2: 0.6 × 0.5 = **0.30**
> - Ở T3: không có b → **0**
> - **Tổng: 1.02**
>
> Quy tắc: nếu Expected Support ≥ minsup thì tập đó phổ biến. Với minsup = 1.0, tập {a, b} ở đây phổ biến vì 1.02 ≥ 1.0.
>
> Một con số duy nhất, một phép so sánh duy nhất — đơn giản và đủ ý nghĩa thống kê.

**Chuyển slide:** *"Với nền tảng đã có, mình đi vào ví dụ thuật toán đầu tiên: UGenMax."*

---

## Slide 12 — UGenMax Example (≈70 giây)

> Đây là ví dụ chạy UGenMax với **minsup = 1.0** trên bộ dữ liệu nhỏ T1, T2, T3.
>
> **Bước 1 — Xây dựng weighted tidset cho từng item.** Mỗi tidset là danh sách (giao dịch, xác suất):
> - a: T1(0.9), T2(0.8) → expSup = **1.7**
> - b: T1(0.8), T2(0.7), T3(0.6) → expSup = **2.1**
> - c: T1(0.4), T2(0.3), T3(0.5) → expSup = **1.2**
>
> Tất cả đều ≥ 1.0, nên đều phổ biến.
>
> **Bước 2 — Sắp xếp item theo expSup tăng dần:** c, a, b. Lý do: item phổ biến nhất nằm cuối, sẽ được xử lý sau cùng trong DFS → các MFI dài xuất hiện sớm hơn → cắt tỉa hiệu quả hơn.
>
> **Bước 3 — DFS** (em chỉ tay vào pseudocode bên phải để dẫn mắt khán giả). Đi từ phải qua trái:
> - Thử **b**: ok, mở rộng tiếp.
> - {b, c} = 0.83 < 1.0 → **PRUNE 1** (cắt do anti-monotone).
> - Thử **a**: ok. {a, b} = 1.28 ≥ 1.0 → thêm vào MFI.
> - {a, c} = 0.60 < 1.0 → **PRUNE 1**.
> - Thử **c**: 1.20 ≥ 1.0 → thêm {c} vào MFI.
>
> **Kết quả: hai MFI {a, b} = 1.28 và {c} = 1.20.**

**Chuyển slide:** *"Cùng bộ dữ liệu, mình xem UFPMax xử lý thế nào."*

---

## Slide 13 — UFPMax Example (≈60 giây)

**[Phi tiếp tục từ đây]**

> Cùng dữ liệu, cùng minsup = 1.0. UFPMax đi theo cách **ngược lại** với UGenMax.
>
> **Bước 1 — Tính expSup từng item** — tất cả đều phổ biến.
>
> **Bước 2 — Sắp xếp theo expSup giảm dần**: b → a → c. Đây là thứ tự chuẩn của FP-tree — phổ biến nhất trước.
>
> **Bước 3 — Xây dựng CSDL chiếu ban đầu**: mỗi giao dịch được sắp lại theo header order; các item dưới ngưỡng bị loại.
>
> **Bước 4 — Khai phá đệ quy từ đuôi về đầu** (tail-to-head):
> - Với mỗi item, xây dựng **CSDL có điều kiện** chỉ chứa các giao dịch có item đó, với trọng số mới = trọng số cũ × xác suất.
> - CSDL trống → đó là MFI. Là tập con của MFI đã biết → bỏ qua (Pruning 2).
>
> **Kết quả: giống y hệt UGenMax** — {a, b} = 1.28, {c} = 1.20. Đây là minh chứng đầu tiên cho tính đúng đắn: hai thuật toán khác nhau cho cùng tập kết quả.

**Chuyển slide:** *"Bây giờ đến phần đóng góp chính của đồ án: chế độ Top-K."*

---

## Slide 14 — Why Top-K? (≈55 giây)

> Slide này giải thích vấn đề của ngưỡng minsup truyền thống:
>
> - **Người dùng phải tự đoán minsup**. Nhưng giá trị nào là "tốt"? Không có công thức.
> - Mỗi bộ dữ liệu lại cần minsup khác nhau — không có giá trị chung.
> - **Quá cao** → 0 kết quả. **Quá thấp** → hàng nghìn kết quả.
>
> Nhìn bảng minh hoạ bên phải:
>
> - **Mushroom**: minsup 500 → 314 MFI, minsup 3000 → chỉ 5 MFI.
> - **Retail**: minsup 1000 → 18 MFI, minsup 10000 → 2 MFI.
> - **Accidents**: minsup 20000 → 1285 MFI, minsup 70000 → 48 MFI.
>
> **Cùng một thuật toán nhưng kết quả phụ thuộc rất lớn vào minsup.** Mà người dùng không phải lúc nào cũng là chuyên gia trong bộ dữ liệu để chọn ngưỡng phù hợp.
>
> Như câu trích bên dưới: *"Người dùng không nên phải là chuyên gia trong bộ dữ liệu mới dùng được thuật toán."*

**Chuyển slide:** *"Đó là lý do tụi em đưa ra chế độ Top-K."*

---

## Slide 15 — Top-K Mode: Just Specify K (≈40 giây)

> Ý tưởng Top-K rất đơn giản: **thay vì chọn minsup, người dùng chỉ cần nói "tôi muốn K kết quả tốt nhất"** — thuật toán tự khám phá ngưỡng phù hợp.
>
> Câu lệnh ví dụ:
>
> ```
> java Main -algorithm UGenMax -input data.txt -topk 10
> ```
>
> Tức là tìm 10 MFI có expSup cao nhất. Ba lợi ích:
>
> - **Không cần tinh chỉnh minsup** — thuật toán tự khám phá ngưỡng qua giai đoạn seeding.
> - **Chất lượng kết quả không đổi** — vẫn là MFI, chỉ giới hạn số lượng.
> - **Thích nghi với từng bộ dữ liệu** — cùng K = 10 chạy được trên cả mushroom, retail, lẫn accidents.

**Chuyển slide:** *"Vậy bên trong nó hoạt động ra sao?"*

---

## Slide 16 — How Top-K Works (UGenMax) (≈60 giây)

> Cốt lõi của Top-K trong UGenMax là **Dynamic Threshold Raising — Nâng ngưỡng động** — gồm 4 bước:
>
> **① Seeding** — Trước khi DFS, tính trước expSup của các tập đơn và tập đôi từ **50 item phổ biến nhất**. Lấy giá trị xếp hạng K làm ngưỡng gốc.
>
> **② Initial minsup** — Đặt minsup ban đầu = **ngưỡng gốc × 0.5** (giảm 50% làm hệ số an toàn, để cho phép DFS khám phá MFI nhiều item).
>
> **③ DFS + TopKHeap** — Dùng min-heap kích thước K, luôn sắp xếp theo expSup.
>
> **④ Dynamic raising** — **Mỗi khi tìm thấy MFI tốt hơn min của heap, minsup được nâng lên** → cắt tỉa mạnh hơn ở các bước sau.
>
> Nó tạo thành **vòng lặp phản hồi (feedback loop)**:
>
> > *Tìm MFI tốt hơn → Nâng minsup → Cắt tỉa mạnh hơn → Hội tụ nhanh hơn → Lặp lại.*

**Chuyển slide:** *"Tiếp theo mình minh hoạ bằng ví dụ cụ thể với K = 2."*

---

## Slide 17 — Top-K Example (K=2) — Part 1: Setup (≈55 giây)

> Cùng bộ dữ liệu T1, T2, T3 như trước. Mục tiêu: tìm **2 MFI tốt nhất** (K = 2).
>
> **Bước 1 — Tính expSup cho mọi tập đơn và tập đôi:**
>
> - Đơn: a = 1.70, b = 2.10, c = 1.20
> - Đôi: {a,b} = 1.28, {a,c} = 0.60, {b,c} = 0.83
>
> **Bước 2 — Sắp xếp giảm dần để được Seed Pool.** Hạng 2 (vì K = 2) chính là **a = 1.70** → đó là **ngưỡng gốc (seeded threshold)**.
>
> **Initial minsup = 1.70 × 0.5 = 0.85.**
>
> Tại sao không lấy ngay 1.70 làm minsup? Vì seed chỉ là tập 1–2 item; các MFI thật có thể nhiều item hơn và do anti-monotone, expSup thấp hơn. Hệ số 0.5 cho phép DFS khám phá sâu hơn mà vẫn giữ lực cắt tỉa đáng kể.

**Chuyển slide:** *"Với ngưỡng 0.85 đã có, DFS bắt đầu."*

---

## Slide 18 — Top-K Example (K=2) — Part 2: DFS (≈55 giây)

> DFS chạy với minsup = 0.85.
>
> - Thử **b** → ok. Mở rộng {b, c}: expSup = 0.83 < 0.85 → **PRUNE 1**. Vì {b} đã thử mở rộng và không thành công → thêm {b} vào heap.
> - Thử **a** → ok. {a, b}: 1.28 ≥ 0.85 → thêm {a, b} vào heap.
> - {a, c}: 0.60 < 0.85 → **PRUNE 1**.
> - Thử **c**: 1.20 ≥ 0.85 → thêm {c} vào heap.
>
> Heap đầy với {a, b} = 1.28 và {c} = 1.20. **Minsup được nâng lên 1.20** — chính là min của heap.
>
> **Kết quả Top-2: {a, b} và {c}.**
>
> Điểm quan trọng: từ minsup ban đầu mà người dùng không hề chỉ định, **thuật toán tự khám phá ra ngưỡng 1.20** — không cần bất kỳ tham số nào từ người dùng. Đây chính là tinh thần của Top-K mode.

**Chuyển slide:** *"Bây giờ tụi em chuyển sang phần thực nghiệm trên dữ liệu thực."*

---

## Slide 19 — Experimental Setup (≈50 giây)

> Tụi em đánh giá trên **3 bộ dữ liệu chuẩn từ thư viện SPMF**:
>
> - **Mushroom** — 8,416 giao dịch, 120 item — **dày**.
> - **Retail** — 88,162 giao dịch, 16,470 item — **thưa**.
> - **Accidents** — 340,183 giao dịch, 468 item — **rất dày**.
>
> Ba bộ này phủ đủ ba *chế độ* dữ liệu thường gặp.
>
> **Cấu hình:**
>
> - **Java SE 25**, không thư viện ngoài.
> - **Xác suất** sinh từ phân phối chuẩn N(0.5, 0.2), giới hạn [0.1, 1.0] — mô phỏng dữ liệu trung bình nhiễu.
> - Mỗi thí nghiệm: **1 warm-up + 3 lần đo, lấy trung bình**.
> - Đầu ra **tương thích định dạng SPMF**.

**Chuyển slide:** *"Đầu tiên là kết quả về thời gian chạy với chế độ minsup tĩnh."*

---

## Slide 20 — Static minsup: Runtime (≈55 giây)

> Ba biểu đồ tương ứng 3 bộ dữ liệu. Trục hoành là minsup, trục tung là thời gian.
>
> **Quan sát chính:**
>
> - **UGenMax (xanh lá) nhanh hơn UFPMax (xanh dương) ở mọi điểm, trên cả 3 bộ dữ liệu.**
> - **Khoảng cách càng nới rộng khi minsup giảm** — lúc đó có nhiều candidate cần cắt hơn, và cắt tỉa qua tidset của UGenMax hiệu quả hơn projection của UFPMax.
> - Trên **accidents** (340K giao dịch): UGenMax chạy từ 6 giây đến 109 giây, UFPMax từ 11.7 đến 126 giây — chênh tới gần một phút ở minsup thấp.
> - **Tốc độ trung bình: UGenMax nhanh hơn 1.3–1.6 lần** trên tất cả các bộ dữ liệu.

**Chuyển slide:** *"Tiếp theo là so sánh về bộ nhớ."*

---

## Slide 21 — Static minsup: Memory (≈55 giây)

> Bộ nhớ thì câu chuyện hơi khác — và tụi em xin nói thẳng là nhiễu khá nhiều, không có pattern tuyệt đối.
>
> - **Retail (UGenMax thắng rõ):** 59–67 MB so với 91–104 MB của UFPMax. Lý do: dữ liệu thưa thì tidset của UGenMax rất gọn.
> - **Mushroom:** cả hai dao động trong khoảng 9–77 MB, đường biểu diễn nhiễu — không có quy luật rõ. Phần lớn do GC biến động trên workload nhỏ.
> - **Accidents:** cả hai trong khoảng 700–1500 MB, không thuật toán nào thắng rõ. Trên dữ liệu rất dày, **weighted tidset của UGenMax phình to** vì hầu hết giao dịch chứa hầu hết item.
>
> **Kết luận về bộ nhớ:** lợi thế của UGenMax phụ thuộc mật độ dữ liệu — tidset chỉ rõ ràng tốt hơn trên dữ liệu thưa.

**Chuyển slide:** *"Slide cuối của phần thực nghiệm — và là điểm UGenMax thắng đậm nhất: chế độ Top-K."*

---

## Slide 22 — Top-K Mode: Where UGenMax Wins Big (≈50 giây)

> Đây là nơi UGenMax thực sự bứt phá.
>
> Ba biểu đồ thể hiện thời gian chạy theo K trên 3 bộ dữ liệu.
>
> - **UGenMax (xanh lá) thấp hơn UFPMax (xanh dương) gần một bậc** trên nhiều cấu hình.
> - **Speedup trung bình: 4.9× trên mushroom, 20.2× trên retail, 8.4× trên accidents.**
> - Ví dụ rõ nhất: **retail K=100** — UGenMax 4.7 giây, UFPMax 24 giây — **nhanh hơn ~5 lần**.
>
> **Lý do nằm ở sự khác biệt thiết kế:** UGenMax **nâng ngưỡng động trong khi tìm kiếm** (Pruning 3), còn UFPMax chỉ dùng ngưỡng seed cố định + post-filter — không cắt tỉa được trong DFS.

**Chuyển slide:** *"Em xin tổng kết lại bài thuyết trình."*

---

## Slide 23 — Conclusion (≈50 giây)

> Tổng kết những đóng góp chính của đồ án:
>
> **①** Cài đặt **hai thuật toán bằng Java SE 25 thuần** cho bài toán khai phá MFI trên dữ liệu không chắc chắn.
>
> **②** **UGenMax vượt UFPMax về thời gian** trên cả 3 bộ dữ liệu — **1.3–1.6× ở chế độ minsup tĩnh, 4.9–20.2× ở Top-K**.
>
> **③** **Bộ nhớ**: UGenMax thắng rõ trên retail; tương đương trên mushroom và accidents.
>
> **④** **Chế độ Top-K loại bỏ việc tinh chỉnh tham số** — người dùng chỉ cần chỉ định K, thuật toán tự khám phá ngưỡng.
>
> **⑤** **Nâng ngưỡng động (Dynamic Threshold Raising)** là điểm tối ưu mấu chốt khiến chế độ Top-K trở nên khả thi.
>
> Bài trình bày của nhóm em xin được kết thúc tại đây. **Em xin chân thành cảm ơn thầy cô đã lắng nghe. Tụi em rất sẵn lòng nhận câu hỏi và nhận xét từ hội đồng.**

---

## 🙋 Mẹo cho phần Q&A

**Nguyên tắc trả lời:**
- Trả lời **ngắn gọn trước, mở rộng sau** nếu hội đồng cần.
- Nếu không chắc câu trả lời: *"Đây là một câu hỏi hay, em xin được suy nghĩ thêm về điểm đó và sẽ làm rõ trong báo cáo cuối."*
- Nếu câu hỏi liên quan đến phần đối phương đã trình bày, có thể nói: *"Để bạn em trả lời phần này ạ"* — phối hợp ăn ý sẽ ghi điểm.

**Một số câu hỏi hội đồng có thể hỏi và gợi ý trả lời:**

| Câu hỏi | Gợi ý trả lời |
|---|---|
| **Vì sao chọn hệ số 0.5 cho seed minsup?** | Hệ số an toàn — seed chỉ tính trên tập 1–2 item, các MFI thật có thể có nhiều item hơn nên expSup thấp hơn (anti-monotone). 0.5 là điểm cân bằng đã thử nghiệm và đủ tốt trên cả 3 bộ dữ liệu. |
| **Vì sao UFPMax không nâng ngưỡng động như UGenMax?** | Vì thứ tự xử lý của UFPMax (item phổ biến nhất trước) khiến MFI 1-item được phát hiện rất sớm. Nếu nâng ngưỡng theo MFI 1-item, ngưỡng sẽ bị đẩy lên rất cao và chặn các MFI nhiều item — sai về mặt đúng đắn. UGenMax đi từ item ít phổ biến nhất trước nên không gặp vấn đề này. |
| **Vì sao bộ nhớ trên mushroom và accidents lại nhiễu?** | Vì PerformanceTracker đọc memory tại 1 thời điểm (start/end) chứ không phải peak. JVM còn GC bất định và heap dùng chung qua các lần đo trong cùng JVM. Trên workload nhỏ (mushroom) thì noise JVM lấn át; trên accidents thì hai thuật toán phình ở giai đoạn khác nhau nên kết quả tuỳ thời điểm chụp. |
| **Tại sao UGenMax thắng đậm hơn ở Top-K so với minsup tĩnh?** | Vì ở minsup tĩnh, ngưỡng được người dùng cố định ngay từ đầu nên cả hai thuật toán đều cắt tỉa hiệu quả. Ở Top-K, UGenMax nâng ngưỡng động trong DFS — cắt tỉa mạnh hơn nhiều ở giữa chừng — còn UFPMax phải chạy gần như toàn bộ không gian tìm kiếm với ngưỡng seed cố định. |
| **Có thể song song hoá không?** | Có. Mỗi nhánh DFS độc lập nên có thể chia cho nhiều thread. Đây là một trong các hướng phát triển mà nhóm em đề cập trong báo cáo. |
| **Vì sao chọn N(0.5, 0.2) cho xác suất?** | Đây là cấu hình "trung bình nhiễu" được dùng phổ biến trong nghiên cứu dữ liệu không chắc chắn (Chui et al. 2007, Leung et al. 2008). Phân phối có nghĩa rõ ràng, không quá tự tin cũng không quá nhiễu. |
| **Hai thuật toán cho ra kết quả giống nhau như thế nào?** | Tụi em đã kiểm chứng cross-check: trên cả 3 bộ dữ liệu và mọi cấu hình minsup/K, UGenMax và UFPMax cho ra **tập MFI giống y hệt** (chỉ khác thứ tự xuất). Đây là phép kiểm tra đúng đắn quan trọng nhất. |
| **Hạn chế của đồ án?** | (1) Maximality check dùng linear scan, có thể tối ưu bằng trie/hash. (2) Bộ nhớ trên accidents có thể lên ~1.5 GB — hạn chế ở môi trường nhỏ. (3) Chỉ đánh giá ở 1 mức uncertainty trung bình. Cả ba đều ghi rõ ở mục Future Work. |

---

## 🎯 Kiểm tra cuối trước khi lên trình bày

- [ ] Đã chạy thử slide trên đúng máy + máy chiếu.
- [ ] Đã backup file PDF và bản .pptx trên USB / cloud.
- [ ] Đã in script ra giấy (phòng khi máy lỗi).
- [ ] Đã uống đủ nước, không ăn quá no.
- [ ] Đã chuẩn bị tinh thần: hội đồng hỏi là vì quan tâm — không phải để "bắt bí".

**Chúc nhóm em trình bày thành công! 🎓**
