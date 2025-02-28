package com.project.demo.controller;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.google.zxing.WriterException;
import com.project.demo.entity.History;
import com.project.demo.entity.User;
import com.project.demo.model.UserBean;
import com.project.demo.service.HistoryService;
import com.project.demo.service.UserService;
import com.project.demo.utils.PasswordGenerator;
import com.project.demo.utils.qrCodeGenerator;

@RestController
public class ProfileController {

	@Autowired
	HistoryService historyService;

	@Autowired
	UserService userService;

	BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();

	public UserBean checkSessionUser(HttpSession session) {
		UserBean bean = null;
		bean = (UserBean) session.getAttribute("user");
		return bean;
	}

	@GetMapping("/profile")
	public ModelAndView goToProfile(HttpSession session, Model model) {
		UserBean userBean = (UserBean) session.getAttribute("user");
		String qrData = "Code = " + userBean.getUserCode() + "\nName = " + userBean.getUserName()
				+ "\nEmail = " + userBean.getUserEmail() + "\nPhone Number = " + userBean.getUserMobile()
				+ "\nRole = " + userBean.getRole();
		byte[] image = new byte[0];

		try {

			image = qrCodeGenerator.getQRCodeImage(qrData, 250, 250);
		} catch (WriterException | IOException e) {
			e.printStackTrace();
		}

		List<History> histories = historyService.listLast20History(userBean.getUserId());

		model.addAttribute("histories", histories);


		String qrcode = Base64.getEncoder().encodeToString(image);

		model.addAttribute("qrcode", qrcode);
		return new ModelAndView("profile", "userBean", userBean);
	}

	@PostMapping(value = "/updateUserInformation")
	public ModelAndView updateUser(@ModelAttribute("userBean") @Valid UserBean bean, BindingResult bs, ModelMap model,
			RedirectAttributes ra, HttpSession session) {

		System.out.println("user name => " + userService.getById(bean.getUserId()).getUserName());

		if (checkSessionUser(session) == null) {
			ra.addFlashAttribute("error", "Please login first !");
			return new ModelAndView("redirect:/login");
		} else if (bs.hasErrors()) {
			model.addAttribute("error", "Field cannot be blank !");
			return new ModelAndView("editUser");
		} else if (userService.findUserName(bean.getUserName()) != null
				&& !userService.getById(bean.getUserId()).getUserName().equals(bean.getUserName())) {
			
			ra.addFlashAttribute("message", "User name is has been !");
		} else if (userService.findUserEmail(bean.getUserEmail()) != null
				&& !userService.getById(bean.getUserId()).getUserEmail().equals(bean.getUserEmail())) {
			
			ra.addFlashAttribute("message", "User email has been taken !");
		} else {
			User u = User.builder()
					.userId(bean.getUserId())
					.userName(bean.getUserName())
					.userEmail(bean.getUserEmail())
					.userMobile(bean.getUserMobile())
					.build();
			User cUser = userService.updateUser(u);

			if (cUser == null) {
				model.addAttribute("message", "Update User Fail !");
				generateHistoryForLoginUser(cUser, session, "tried to update");
				return new ModelAndView("editUser");
			} else {
				ra.addFlashAttribute("message", "Update User Successful !");
				generateHistoryForLoginUser(cUser, session, "updated");
				UserBean loginUser = (UserBean) session.getAttribute("user");
				if (loginUser.getUserCode().equals(bean.getUserCode())) {
					loginUser.setUserName(bean.getUserName());
					loginUser.setUserEmail(bean.getUserEmail());
					loginUser.setUserMobile(bean.getUserMobile());
					session.setAttribute("loginUser", loginUser);
				}
			}

		}
		return new ModelAndView("redirect:/profile");
	}

	@PostMapping(value = "/changePassword")
	public ModelAndView changePassword(@RequestParam("userId") String userId, @RequestParam("oldPassword") String oldPw,
			@RequestParam("newPassword") String newPw, @RequestParam("confirmPassword") String conPw, ModelMap model,
			RedirectAttributes ra, HttpSession session) {

		User user = userService.getById(Long.parseLong(userId));

		boolean isPasswordMatches = bcrypt.matches(oldPw, user.getPassword());

		if (!isPasswordMatches) {
			ra.addFlashAttribute("message", "Wrong Old Password!");
			generateHistoryForLoginUser(user, session, "tried to change password for");
		} else if (!newPw.equals(conPw)) {
			ra.addFlashAttribute("message", "New Password and confirm pw are not same.");
			generateHistoryForLoginUser(user, session, "tried to change password for");
		} else {
			user.setPassword(PasswordGenerator.generatePassword(newPw));
			User result = userService.changePassword(user);
			if (result != null) {
				ra.addFlashAttribute("message", "Successfully Changed Password");
				generateHistoryForLoginUser(user, session, "changed password for");
			}
		}

		return new ModelAndView("redirect:/profile");

	}

	public void generateHistoryForLoginUser(User user, HttpSession session, String action) {

		UserBean userBean = (UserBean) session.getAttribute("user");
		User user2 = userService.getById(userBean.getUserId());

		String data = user.getUserId() + " , " + user.getUserCode() + " , " + user.getUserName();

		History history = History.builder()
				.user(user2)
				.action(action)
				.dataName(user.getUserName())
				.tableName("User")
				.data(data)
				.historyCreatedTime(Timestamp.valueOf(LocalDateTime.now()))
				.build();

		historyService.createHistory(history);
	}

}
