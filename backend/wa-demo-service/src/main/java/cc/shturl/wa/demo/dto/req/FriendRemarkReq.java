package cc.shturl.wa.demo.dto.req;

import jakarta.validation.constraints.Size;

public record FriendRemarkReq(@Size(max = 50, message = "备注最多50个字符") String remarkName) {
}
