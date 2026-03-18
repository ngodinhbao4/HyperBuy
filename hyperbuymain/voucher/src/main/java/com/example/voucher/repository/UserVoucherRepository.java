package com.example.voucher.repository;

import com.example.voucher.entity.UserVoucher;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserVoucherRepository extends JpaRepository<UserVoucher, Long> {

    Optional<UserVoucher> findFirstByUserIdAndVoucher_CodeAndUsedFalseOrderByIdAsc(
            String userId,
            String code
    );
    
    List<UserVoucher> findByUserId(String userId);

    List<UserVoucher> findByUserIdAndUsedFalse(String userId);


}
