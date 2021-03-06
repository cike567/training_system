package com.ruoyi.cms.controller;

import com.github.pagehelper.PageInfo;
import com.ruoyi.common.base.AjaxResult;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.utils.PaginUtil;
import com.ruoyi.exam.domain.*;
import com.ruoyi.exam.service.IExamPracticeQuestionService;
import com.ruoyi.exam.service.IExamPracticeService;
import com.ruoyi.exam.service.IExamQuestionService;
import com.ruoyi.framework.web.page.PageDomain;
import com.ruoyi.framework.web.page.TableSupport;
import com.ruoyi.framework.web.util.ShiroUtils;
import com.ruoyi.system.domain.SysUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.*;

/**
 * Created by flower on 2019/1/18.
 */
@Controller
@RequestMapping("/web")
public class CmsPracticeController {

    @Autowired
    private IExamPracticeService examPracticeService;


    @Autowired
    private IExamQuestionService examQuestionService;

    @Autowired
    private IExamPracticeQuestionService examPracticeQuestionService;


    private String prefix = "web/practice/";

    @RequestMapping("/practice")
    @GetMapping()
    public String list(ModelMap map) {
        SysUser user = ShiroUtils.getSysUser();
        if (ObjectUtils.isEmpty(user) || !UserConstants.USER_VIP.equals(user.getUserType())){
            return "web/user/login";
        }
        map.put( "user", user);
        return prefix + "list";
    }

    /**
     * 查询练习列表
     * @param examPractice
     * @return
     */
    @GetMapping("/practice/list")
    @ResponseBody
    public AjaxResult list(ExamPracticeVO examPractice) {
        SysUser user = ShiroUtils.getSysUser();
        if (ObjectUtils.isEmpty(user) || !UserConstants.USER_VIP.equals(user.getUserType())){
            AjaxResult fail = AjaxResult.error("请登录");
            return fail;
        }
        examPractice.setVipUserId(user.getUserId().toString());
        List<ExamPracticeVO> list = examPracticeService.selectListFromWeb(examPractice);

        // 服务端分页
        PageDomain pageDomain = TableSupport.buildPageRequest();
        Integer pageNum = pageDomain.getPageNum();
        Integer pageSize = pageDomain.getPageSize();
        Map<String,Object> reslutMap = PaginUtil.getPagingResultMap(list,pageNum,pageSize);

        AjaxResult success = AjaxResult.success( "查询成功" );
        success.put( "data", reslutMap.get("result") );
        success.put("pages",reslutMap.get("totalPageNum"));
        success.put("total",reslutMap.get("totalRowNum"));
        return success;
    }

    @RequestMapping("/practice/start/{id}")
    @GetMapping()
    public String start(@PathVariable String id, ModelMap mmap) {
        SysUser user = ShiroUtils.getSysUser();
        if (ObjectUtils.isEmpty(user) || !UserConstants.USER_VIP.equals(user.getUserType())){
            return "web/user/login";
        }
        HashMap<String, Object> map = new HashMap<>();
        map.put("practiceId", id);
        List<ExamQuestionVO> result = examQuestionService.selectQuestionListByPracticeId(map);
        if (map.containsKey("disorder") && map.get("disorder").toString().equals("1")) {
            Collections.shuffle(result);
        }
        mmap.put("data", result);
        mmap.put("practice",examPracticeService.selectById(id));
        return prefix + "detail";
    }
}
