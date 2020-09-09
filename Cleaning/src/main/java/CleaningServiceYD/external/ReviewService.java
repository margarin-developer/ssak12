
package CleaningServiceYD.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@FeignClient(name="Review", url="${api.url.review}")
public interface ReviewService {

    @RequestMapping(method= RequestMethod.POST, path="/reviews")
    public void reviewRequest(@RequestBody Review review);

}