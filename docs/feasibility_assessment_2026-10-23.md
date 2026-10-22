# Đánh giá khả thi BattleCode-UET (23/10/2026)

## Kết luận

Có thể xây dựng cổng thi đấu để nhiều sinh viên truy cập qua Internet và tổ chức một giải thử ngắn hạn với chi phí dịch vụ bằng 0, **nếu có được một máy ảo cloud miễn phí cho worker chạy mã bot** và kiểm chứng sandbox trên chính máy đó. Bộ tài liệu hiện tại là bản định hướng kiến trúc, chưa phải mã nguồn hay đặc tả đủ để triển khai trận đấu công bằng. Không có cơ sở để cam kết ngay quy mô 2.000 đội, thi liên tục 24/7 hoặc bảo đảm tuyệt đối không phát sinh phí trên mọi dịch vụ.

Thư mục dự án hiện chỉ có bảy tài liệu trong `docs` và các tệp `.agent`; chưa có engine, backend, frontend, starter kit, migration, test hay Git repository. Người tổ chức xác nhận chỉ dùng dịch vụ cloud miễn phí, không có máy Linux của UET để chạy trận; mã bot trong giải thử có thể ở GitHub repository công khai.

## Đối chiếu với tài liệu hiện có

| Tài liệu | Phần đã mô tả | Phần còn cần trước khi tổ chức giải |
| --- | --- | --- |
| `doc1_plugin_sandbox_replay.md` | Hợp đồng plug-in, NDJSON, thời gian mỗi lượt, replay | Kiểu và schema thực tế của `BotObservation`/`BotAction`; quy tắc xử lý JSON lỗi; định dạng delta/keyframe. Ví dụ replay hiện ghi toàn bộ thực thể mỗi tick, chưa thể hiện delta. |
| `doc2_system_architecture.md` | Thành phần web/API/DB/CI, bảng dữ liệu, mẫu claim queue | Migration, RLS/RBAC phía server, cấp quyền worker, idempotency khi lease hết hạn, heartbeat, giới hạn retry. |
| `doc3_multi_runtime_sandbox.md` | Python/C++/Java, process sống suốt trận, stdin/stdout | Cấu hình sandbox chạy được trên nhà cung cấp thực tế, cgroup giới hạn RAM/CPU, kiểm thử Java với 96 MB và khởi động 1 giây. `bwrap` tự nó không thiết lập quota RAM. |
| `doc4_games_cybergrid_stellar.md` | Đơn vị, luật tick của Cyber-Grid, vật lý Stellar Armada | Sinh bản đồ đối xứng, số tài nguyên ban đầu, action/observation, tầm nhìn và thông tin bị che, tính điểm/hòa, thứ tự xử lý các trường hợp va chạm. |
| `doc5_limits_and_sop.md` | Quota dự kiến và quy trình giải lớn | Ước lượng 2.000 đội và 30 phút phụ thuộc 10–20 máy phòng lab, trái với điều kiện cloud miễn phí đã chốt. Cần benchmark thời gian trận, kích thước replay và sức chứa thực tế. |
| `doc6_ui_design_guide.md`, `ui_theme_manifest.md` | Các màn hình, quyền, giao diện và màu sắc | Nhiều route động không đi thẳng với Next.js static export trên Cloudflare Pages. Tệp `docs/thiet_ke_giao_dien.md` được tham chiếu nhưng không tồn tại. |

Các kiểm tra chuỗi cấm như `eval`, `exec`, `socket` chỉ là kiểm tra đầu vào; không thể thay sandbox hệ điều hành. Không thể tin kết quả CI do workflow trong repository của thí sinh tự báo về; pipeline chính thức phải do ban tổ chức kiểm soát và khóa đúng commit SHA.

## Giới hạn dịch vụ đã kiểm tra

| Dịch vụ | Điều có thể dùng | Ràng buộc ảnh hưởng đến thiết kế |
| --- | --- | --- |
| Render Free | Java API công khai qua HTTPS | 0,1 CPU và 512 MB RAM; ngủ sau 15 phút không có yêu cầu; khởi động lại có thể mất khoảng một phút; filesystem tạm; không có background worker miễn phí. Chưa xác minh được `bwrap`/`nsjail` hoạt động trong môi trường Render Free. [Compute plans](https://render.com/docs/compute-plans), [Free services](https://render.com/docs/free). |
| Cloudflare Pages | Frontend tĩnh, CDN, 500 build/tháng | Next.js App Router trên Pages theo tài liệu hiện hành là static export; route động và SSR cần phương án khác hoặc giản lược route. [Pages Next.js static export](https://developers.cloudflare.com/pages/framework-guides/nextjs/deploy-a-static-nextjs-site/), [Pages limits](https://developers.cloudflare.com/pages/platform/limits/). |
| Supabase Free | Auth, PostgreSQL 500 MB | Realtime tối đa 200 kết nối đồng thời; dự án ít hoạt động có thể bị tạm dừng sau 7 ngày. Vì vậy không thể để 2.000 người đồng thời mở kết nối Realtime; dùng HTTP polling/cached leaderboard ở giải lớn. [Billing](https://supabase.com/docs/guides/platform/billing-on-supabase), [Realtime limits](https://supabase.com/docs/guides/realtime/limits), [Project pausing](https://supabase.com/docs/guides/platform/free-project-pausing). |
| Cloudflare R2 Standard | 10 GB-tháng, 1 triệu ghi và 10 triệu đọc miễn phí mỗi tháng | Phải kích hoạt R2 subscription; vượt quota có thể bị tính phí. Cần giới hạn dung lượng, thời gian giữ replay và theo dõi sử dụng. [R2 pricing](https://developers.cloudflare.com/r2/pricing/), [R2 setup](https://developers.cloudflare.com/r2/get-started/). |
| GitHub Actions | Build và smoke test bot trong repository công khai bằng runner tiêu chuẩn | Không dùng Actions làm worker chạy các trận cho ứng dụng: điều khoản cấm dùng như một phần của ứng dụng serverless hoặc việc không liên quan đến phát triển/kiểm thử/xuất bản dự án trong repository. Job có giới hạn thời gian và runner có thể bị hạn chế. [Actions billing](https://docs.github.com/en/billing/concepts/product-billing/github-actions), [Terms](https://docs.github.com/en/site-policy/github-terms/github-terms-for-additional-products-and-features), [Limits](https://docs.github.com/en/actions/reference/limits). |
| Oracle Cloud Always Free (worker đề xuất) | Máy ảo Arm A1 tối đa tổng cộng 2 OCPU và 12 GB RAM cho tài khoản Always Free | Có thể báo hết chỗ ở vùng đăng ký và máy nhàn rỗi có thể bị thu hồi. Tài khoản Free Tier không bị tính phí nếu không nâng cấp, nhưng cần xác minh tài khoản; phải chạy thử sandbox và bot Java/C++/Python trên Arm. [Always Free resources](https://docs.oracle.com/en-us/iaas/Content/FreeTier/freetier_topic-Always_Free_Resources.htm), [Free Tier account](https://docs.oracle.com/en-us/iaas/Content/FreeTier/freetier.htm). |

Với 2.000 đội, 7 vòng Swiss và 3 trận mỗi cặp, riêng giải đã có khoảng **21.000 trận**. Nếu mỗi đội nộp 15 lần và mỗi lần CI mất 25–40 giây, đó là 12.500–20.000 phút CI. Số liệu trong `doc5` không chứng minh được khả năng xử lý với chỉ dịch vụ cloud miễn phí; quota 2.000 phút cho repository private của GitHub Free sẽ không đủ. Repository public miễn phí về phút chạy runner tiêu chuẩn, nhưng vẫn không được biến Actions thành máy chủ xử lý giải.

## Phương án triển khai cho giải thử

1. **Cổng công khai:** Cloudflare Pages phục vụ giao diện; Render Free chạy Java 21 API cho đăng nhập, đăng ký đội, chọn commit bot, công bố trạng thái/trận/kết quả; Supabase Free giữ Auth và PostgreSQL. Giao diện cần được thiết kế như site tĩnh hoặc triển khai Next.js động bằng phương án khác đã kiểm tra quota.
2. **Nộp bot:** Mỗi đội khai báo GitHub repository công khai và commit SHA. CI do ban tổ chức kiểm soát tải đúng SHA, kiểm tra cấu trúc, biên dịch, chạy smoke test trong môi trường cô lập và lưu artifact có checksum. Nếu worker dùng VM Arm, C++ và Java phải được build/test trên runner Arm tương ứng (GitHub có `ubuntu-24.04-arm` miễn phí cho repository công khai) hoặc build đa kiến trúc. Không chạy workflow do thí sinh chỉnh sửa để cấp quyền vào hệ thống. [GitHub-hosted runners](https://docs.github.com/en/actions/reference/runners/github-hosted-runners).
3. **Worker chính thức:** Một VM Oracle Always Free chạy Java 21 match runner và sandbox Linux cho từng bot, nhận job từ PostgreSQL bằng lease/idempotency, giới hạn mạng, file, CPU, RAM và thời gian. Worker không chứa token trong môi trường của bot. Render chỉ làm API; không chạy mã thí sinh trên cùng process/container API.
4. **Giải đầu tiên:** Chỉ Cyber-Grid, 2 đội mỗi trận, tối đa 16 đội để chạy tổng duyệt; loại trực tiếp Bo3, tối đa 45 trận. Sau khi benchmark và thử khôi phục lỗi, có thể mở đến 32 đội, tối đa 93 trận. Kết quả chốt theo commit SHA và seed; hai ván đầu đổi vị trí trên cùng bản đồ, ván thứ ba dùng seed công bố trước. Chỉ công bố replay sau khi ván chính thức hoàn tất.
5. **Kho dữ liệu:** PostgreSQL chỉ lưu metadata. Artifact, log và replay nén được lưu trong R2 với giới hạn dung lượng và thời hạn lưu; dùng URL có thời hạn cho dữ liệu chưa công bố. Nếu R2 subscription không phù hợp yêu cầu chi phí tuyệt đối bằng 0, cần duyệt thay đổi invariant lưu trữ trong `.agent/00_CONSTITUTION.md` trước khi thay bằng kho khác.

## Điều kiện để có thể tuyên bố sẵn sàng tổ chức giải

- Có VM cloud miễn phí thực sự được cấp phát; runner trên VM chạy được cả ba ngôn ngữ với sandbox và quota theo luật.
- Chốt đặc tả Cyber-Grid còn thiếu, phát hành starter kit ở ba ngôn ngữ và bộ test luật/định dạng bot.
- Hai tài khoản ở hai mạng khác nhau có thể đăng ký, nộp bot, thấy cùng một cặp đấu, chạy trận và xem replay qua URL công khai.
- Test bot cố ý treo, xuất quá 16 KB, tốn RAM, mở mạng và đọc file ngoài sandbox đều bị chặn mà API/worker vẫn phục hồi.
- Cùng commit, cấu hình và seed cho cùng kết quả; trường hợp worker chết và lease hết hạn không ghi hai kết quả.
- Chạy tổng duyệt 16 đội, đo thời gian trận/queue, RAM, dung lượng replay, quota và thời gian khôi phục. Chỉ mở 32 đội nếu số liệu đạt yêu cầu.

**Rủi ro quyết định:** Nếu không cấp được VM miễn phí có sandbox kiểm chứng, cấu trúc Render + Cloudflare + Supabase vẫn làm được cổng nhiều người, nhưng chưa đủ an toàn để tuyên bố có một giải BattleCode chạy mã bot của sinh viên. Giải 2.000 đội và 24/7 cần khảo sát lại sau bản thử và có thêm năng lực tính toán.
