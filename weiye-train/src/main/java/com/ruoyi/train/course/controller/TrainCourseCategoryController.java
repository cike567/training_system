package com.ruoyi.train.course.controller;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.base.AjaxResult;
import com.ruoyi.common.constant.ExamConstants;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.framework.web.exception.base.BaseException;
import com.ruoyi.train.course.domain.TrainCourseCategory;
import com.ruoyi.train.course.service.ITrainCourseCategoryService;
import com.ruoyi.framework.web.base.BaseController;
import com.ruoyi.framework.web.util.ShiroUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

/**
 * 课程分类管理
 *
 * @author ruoyi
 */
@Controller
@RequestMapping("/train/course/category")
public class TrainCourseCategoryController extends BaseController {
    private String prefix = "train/course/category";

    @Autowired
    private ITrainCourseCategoryService trainCourseCategoryService;

    @RequiresPermissions("train:course:category:view")
    @GetMapping()
    public String category() {
        return prefix + "/category";
    }

    @RequiresPermissions("train:course:category:list")
    @GetMapping("/list")
    @ResponseBody
    public List<TrainCourseCategory> list(TrainCourseCategory category) {
        List<TrainCourseCategory> categoryList = trainCourseCategoryService.selectAllCategoryList( category );
        return categoryList;
    }

    /**
     * 新增课程分类
     */
    @GetMapping("/add/{parentId}")
    public String add(@PathVariable("parentId") Long parentId, ModelMap mmap, HttpServletResponse response) throws IOException {
        TrainCourseCategory category = trainCourseCategoryService.selectCategoryById( parentId );
        mmap.put( "category",  category);
        return prefix + "/add";
    }

    /**
     * 新增保存课程分类
     */
    @Log(title = "课程分类管理", businessType = BusinessType.INSERT)
    @RequiresPermissions("train:course:category:add")
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult addSave(TrainCourseCategory category) {
        // 分类名称
        Assert.hasText(category.getName(), "课程分类不能为空！");
        String checkNameUnique = trainCourseCategoryService.checkNameUnique(category.getName(), category.getParentId());
        if (checkNameUnique.equals(ExamConstants.TRAIN_COURSE_CATEGORY_NAME_NOT_UNIQUE)) {
            return error("同一课程分类下分类名称不能重复！");
        }
        String ids = category.getParentIds();
        String[] idsStr = ids.split(",");
        // 目前前台仅支持三级分类
        if(idsStr.length > 4){
            return AjaxResult.error("仅支持三级分类");
        }
        category.setCreateBy( ShiroUtils.getLoginName() );
        return toAjax( trainCourseCategoryService.insertCategory( category ) );
    }

    /**
     * 修改
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        TrainCourseCategory category = trainCourseCategoryService.selectCategoryById( id );
        mmap.put( "category", category );
        return prefix + "/edit";
    }

    /**
     * 保存
     */
    @Log(title = "课程分类管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("train:course:category:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(TrainCourseCategory category) {
        Assert.notNull(category.getId(), "课程分类ID为必填项！");
        Assert.hasText(category.getName(), "课程分类不能为空！");
        TrainCourseCategory categoryOld = trainCourseCategoryService.selectCategoryById(category.getId());
        if(StringUtils.isNotNull(categoryOld) && !category.getName().equals(categoryOld.getName())) {
            String checkNameUnique = trainCourseCategoryService.checkNameUnique(category.getName(), category.getParentId());
            if (checkNameUnique.equals(ExamConstants.TRAIN_COURSE_CATEGORY_NAME_NOT_UNIQUE)) {
                return error("同一课程分类下分类名称不能重复！");
            }
        }
        if (categoryOld.getStatus() != category.getStatus()){
            if (category.getStatus() == 0){
                // 停用变为正常
                category.setUpdateBy( ShiroUtils.getLoginName() );
                return toAjax( trainCourseCategoryService.updateCategory( category ) );
            }else {
                // 正常变为停用
                if (trainCourseCategoryService.checkCategoryExistCourse( category.getId() )) {
                    return error( 1, "课程分类存在用户,不允许停用" );
                }

                //停用当前节点及所有子节点
                trainCourseCategoryService.updateCategoryByParentId(category.getId(), ShiroUtils.getLoginName());
                return AjaxResult.success();
            }
        }else {
            category.setUpdateBy( ShiroUtils.getLoginName() );
            return toAjax( trainCourseCategoryService.updateCategory( category ) );
        }
    }

    /**
     * 删除
     */
    @Log(title = "课程分类管理", businessType = BusinessType.DELETE)
    @RequiresPermissions("train:course:category:remove")
    @PostMapping("/remove/{id}")
    @ResponseBody
    public AjaxResult remove(@PathVariable("id") Long id) {
        if (trainCourseCategoryService.selectCategoryCount( id ) > 0) {
            return error( 1, "存在下级课程分类,不允许删除" );
        }
        if (trainCourseCategoryService.checkCategoryExistCourse( id )) {
            return error( 1, "课程分类存在用户,不允许删除" );
        }
        return toAjax( trainCourseCategoryService.deleteCategoryById( id ) );
    }

    /**
     * 校验课程分类名称
     */
    @PostMapping("/checkCategoryNameUnique")
    @ResponseBody
    public String checkCategoryNameUnique(TrainCourseCategory category) {
        return trainCourseCategoryService.checkCategoryNameUnique( category );
    }

    /**
     * 选择课程分类树
     */
    @GetMapping("/selectCategoryTree/{id}")
    public String selectCategoryTree(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put( "category", trainCourseCategoryService.selectCategoryById( id ) );
        return prefix + "/tree";
    }

    /**
     * 加载课程分类列表树
     */
    @GetMapping("/treeData")
    @ResponseBody
    public List<Map<String, Object>> treeData() {
        List<Map<String, Object>> tree = trainCourseCategoryService.selectCategoryTree();
        return tree;
    }

}
