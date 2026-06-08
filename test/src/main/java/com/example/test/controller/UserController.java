package com.example.test.controller;

import com.example.test.entity.User;
import com.example.test.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 查询所有用户（id > 10）
     * GET /api/user/list
     */
    @GetMapping("/list")
    public List<User> list() {
        return userService.findAll();
    }

    /**
     * 根据条件查询用户（动态 SQL）
     * GET /api/user/search?name=张&minId=5
     */
    @GetMapping("/search")
    public List<User> search(@RequestParam(required = false) String name,
                              @RequestParam(required = false) Integer minId) {
        return userService.findByCondition(name, minId);
    }

    /**
     * 根据 ID 查询用户
     * GET /api/user/{id}
     */
    @GetMapping("/{id}")
    public User getById(@PathVariable Integer id) {
        return userService.findById(id);
    }

    /**
     * 新增用户
     * POST /api/user
     */
    @PostMapping
    public User create(@RequestBody User user) {
        return userService.save(user);
    }

    /**
     * 删除用户
     * DELETE /api/user/{id}
     */
    @DeleteMapping("/{id}")
    public String delete(@PathVariable Integer id) {
        userService.deleteById(id);
        return "删除成功";
    }

    /**
     * 导出用户信息为 Excel
     * GET /api/user/export
     */
    @GetMapping("/export")
    public void export(HttpServletResponse response) {
        userService.exportExcel(response);
    }
}
