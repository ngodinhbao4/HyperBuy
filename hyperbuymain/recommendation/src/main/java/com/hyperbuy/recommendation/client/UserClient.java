@FeignClient(name = "user-service", url = "${USER_SERVICE_URL}")
public interface UserClient {
    @GetMapping("/api/v1/users/{id}")
    UserDTO getUserById(@PathVariable("id") Long id);
}
