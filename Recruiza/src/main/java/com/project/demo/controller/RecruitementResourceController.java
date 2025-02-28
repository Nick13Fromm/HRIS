package com.project.demo.controller;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.repository.query.Param;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.project.demo.entity.History;
import com.project.demo.entity.RecruitementResource;
import com.project.demo.entity.User;
import com.project.demo.model.RecruitementResourceBean;
import com.project.demo.model.UserBean;
import com.project.demo.repository.RecruitmentResourceRepository;
import com.project.demo.service.HistoryService;
import com.project.demo.service.RecruitementResourceService;
import com.project.demo.service.UserService;
import com.project.demo.utils.UIOptionData;

@RestController
public class RecruitementResourceController {

	@Autowired
	RecruitementResourceService service;
	@Autowired
	RecruitmentResourceRepository repo;
	
	@Autowired
	UserService userService;
	
	@Autowired
	HistoryService historyService;

	@PostMapping(value = "/saveresource")
	public ModelAndView saveResource(@ModelAttribute("resource") @Validated RecruitementResourceBean resource,
			HttpSession session, RedirectAttributes ra, BindingResult bs, ModelMap model) {

		if (bs.hasErrors()) {
			ra.addFlashAttribute("message", "Insert Fail!");
			return new ModelAndView("redirect:/recruitementresource");
		}

		RecruitementResource resource1 = RecruitementResource.builder().resourceName(resource.getResourceName())
				.link(resource.getLink()).address(resource.getAddress()).resourceMobile(resource.getResourceMobile())
				.contactPerson(resource.getContactPerson()).resourceCreatedTime(Timestamp.valueOf(LocalDateTime.now()))
				.recruitementType(resource.getRecruitementType()).build();
		
		if(resource.getResourceMobile().length()>8) {
		
		RecruitementResource result=service.createRecruitementResource(resource1);
		if(result != null) {
			
			generateHistoryforResource(result,session,"added");
			ra.addFlashAttribute("message", "Save Successfully!");
		}
		else {
			generateHistoryforResource(result,session,"fail to added");
			ra.addFlashAttribute("message","Insert Fail");
		}
	}else 
		ra.addFlashAttribute("message","Fail to Create Recruitement Resource.Invalid Mobile Number!");
	
		return new ModelAndView("redirect:/recruitementresource");

	}

	@GetMapping(value = "/editresource")
	public ModelAndView displayEditResource(@RequestParam("id") Long id, RedirectAttributes model, ModelMap m) {

		RecruitementResource resource = service.getResourceById(id);
		RecruitementResource bean = RecruitementResource.builder().resourceId(resource.getResourceId())
				.resourceName(resource.getResourceName()).link(resource.getLink()).address(resource.getAddress())
				.resourceMobile(resource.getResourceMobile()).contactPerson(resource.getContactPerson())
				.recruitementType(resource.getRecruitementType()).build();

		m.addAttribute("rList", UIOptionData.generateResourceType());
		return new ModelAndView("editResourceControl", "resource", bean);
	}

	@PostMapping(value = "/updateresource")
	public ModelAndView updateResource(@ModelAttribute("resource") @Validated RecruitementResourceBean resource,
			RedirectAttributes model, BindingResult bs,HttpSession session) {
		if (bs.hasErrors()) {
			model.addFlashAttribute("message", "Update Fail!");
			return new ModelAndView("editResourceControl");
		}
		RecruitementResource bean = RecruitementResource.builder().resourceId(resource.getResourceId())
				.resourceName(resource.getResourceName()).link(resource.getLink()).address(resource.getAddress())
				.resourceMobile(resource.getResourceMobile()).contactPerson(resource.getContactPerson())
				.resourceCreatedTime(Timestamp.valueOf(LocalDateTime.now()))
				.recruitementType(resource.getRecruitementType()).build();

		RecruitementResource result= service.updateRecruitementResource(bean);
		if(result != null) {
			generateHistoryforResource(result,session,"updated");
			model.addFlashAttribute("message", "Updated Successfully!");
			
		}
		else {
			generateHistoryforResource(result, session, "fail to update");
			model.addFlashAttribute("message", "Updated Fail!");
		}
		return new ModelAndView("redirect:/recruitementresource");
	}

	@GetMapping(value = "/deleteresource")
	public ModelAndView deleteResource(ModelMap model, RedirectAttributes ra, @RequestParam("id") Long id, ModelMap m,HttpSession session) {

		try {
			RecruitementResource result = service.getResourceById(id);
			service.deleteRecruitementResource(id);
			generateHistoryforResource(result, session, "deleted");
		} catch (Exception expection) {
			
			model.addAttribute("message", "It seems to be that you can't delete this resource.");
			RecruitementResource resource = service.getResourceById(id);
			
			RecruitementResource bean = RecruitementResource.builder().resourceId(resource.getResourceId())
					.resourceName(resource.getResourceName()).link(resource.getLink()).address(resource.getAddress())
					.resourceMobile(resource.getResourceMobile()).contactPerson(resource.getContactPerson())
					.recruitementType(resource.getRecruitementType()).build();

			generateHistoryforResource(resource, session, "could not delete");
			m.addAttribute("rList", UIOptionData.generateResourceType());
			return new ModelAndView("editResourceControl", "resource", bean);

		}
		ra.addFlashAttribute("message", "delete successfully!");
		return new ModelAndView("redirect:/recruitementresource");
	}

	@PostMapping(value = "/searchresource")
	public ModelAndView searchResource(Model ra, @RequestParam("name") String name) {
		List<RecruitementResource> list = service.getByCodeNameMobileAndType(name, name, name, name);

		ra.addAttribute("list", list);
		ra.addAttribute("rList", UIOptionData.generateResourceType());
		System.out.print(list.toString());
		return new ModelAndView("recruitementResourceControl", "resource", new RecruitementResourceBean());
	}

	@GetMapping(value = "/recruitementresource")
	public ModelAndView addResource(ModelMap model, RedirectAttributes ra, HttpSession session,
			RecruitementResourceBean beanResource) {
		String keyword = null;
		model.addAttribute("rList", UIOptionData.generateResourceType());
		return searchResource(model, ra, session, beanResource, 1, "resourceId", "asc", keyword);
	}

	@GetMapping(value = "/searchResource/{pageNumber}")
	public ModelAndView searchResource(ModelMap model, RedirectAttributes ra, HttpSession session,
			RecruitementResourceBean beanResource, @PathVariable("pageNumber") int currentPage,
			@Param("sortField") String sortField, @Param("sortDir") String sortDir, @Param("keyword") String keyword) {

		int index = Integer.parseInt((currentPage - 1) + "1");
		Page<RecruitementResource> page = service.listAllResources(currentPage, sortField, sortDir, keyword);

		long totalResource = page.getTotalElements();
		int totalPages = page.getTotalPages();

		List<RecruitementResource> list = page.getContent();
		
		model.addAttribute("totalResourcesByUni",service.getCountByType("University"));
		model.addAttribute("totalResourcesByAgency",service.getCountByType("Agency"));
		model.addAttribute("totalResourcesByDirect",service.getCountByType("DirectRecruit"));
		model.addAttribute("totalResourcesByTrainingCenter",service.getCountByType("Training Center"));
		model.addAttribute("totalResourcesByOthers",service.getCountByType("Others"));

		model.addAttribute("currentPage", currentPage);
		model.addAttribute("totalResources", totalResource);
		model.addAttribute("totalPages", totalPages);
		model.addAttribute("list", list);
		model.addAttribute("sortField", sortField);
		model.addAttribute("sortDir", sortDir);
		model.addAttribute("keyword", keyword);
		model.addAttribute("rList", UIOptionData.generateResourceType());
		model.addAttribute("index", index);

		String reverseSortDir = sortDir.equals("asc") ? "desc" : "asc";
		model.addAttribute("reverseSortDir", reverseSortDir);

		return new ModelAndView("recruitementResourceControl", "resource", beanResource);
	}

	public void generateHistoryforResource(RecruitementResource resource, HttpSession session, String action) {

		UserBean userBean = (UserBean) session.getAttribute("user");
		User user = userService.getById(userBean.getUserId());

		String data = resource.getResourceId()+","+resource.getResourceCode()+","+resource.getResourceName()+","
				+resource.getLink()+","+resource.getAddress()+","+resource.getResourceMobile() +","+resource.getContactPerson()
				+","+resource.getRecruitementType();
				;
		History history = History.builder()
				.user(user)
				.action(action)
				.dataName(resource.getResourceName())
				.tableName("Recruitment Resource")
				.data(data)
				.historyCreatedTime(Timestamp.valueOf(LocalDateTime.now())).build();
		historyService.createHistory(history);
	}

}
