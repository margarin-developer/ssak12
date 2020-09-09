package CleaningServiceYD;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DashBoardViewRepository extends CrudRepository<DashBoardView, Long> {

    List<DashBoardView> findByRequestId(Long requestId);

        void deleteByRequestId(Long requestId);
}