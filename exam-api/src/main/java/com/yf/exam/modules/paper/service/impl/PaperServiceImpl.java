package com.yf.exam.modules.paper.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yf.exam.core.api.dto.PagingReqDTO;
import com.yf.exam.core.exception.ServiceException;
import com.yf.exam.core.utils.BeanMapper;
import com.yf.exam.core.utils.StringUtils;
import com.yf.exam.modules.exam.entity.Exam;
import com.yf.exam.modules.exam.service.ExamService;
import com.yf.exam.modules.paper.dto.PaperDTO;
import com.yf.exam.modules.paper.dto.PaperQuDTO;
import com.yf.exam.modules.paper.dto.PaperRuleRepoDTO;
import com.yf.exam.modules.paper.dto.ext.PaperQuAnswerExtDTO;
import com.yf.exam.modules.paper.dto.ext.PaperQuDetailDTO;
import com.yf.exam.modules.paper.dto.request.PaperAnswerDTO;
import com.yf.exam.modules.paper.dto.response.ExamDetailRespDTO;
import com.yf.exam.modules.paper.dto.response.ExamResultRespDTO;
import com.yf.exam.modules.paper.dto.response.PaperPagingRespDTO;
import com.yf.exam.modules.paper.entity.Paper;
import com.yf.exam.modules.paper.entity.PaperQu;
import com.yf.exam.modules.paper.entity.PaperQuAnswer;
import com.yf.exam.modules.paper.entity.PaperRule;
import com.yf.exam.modules.paper.entity.PaperV;
import com.yf.exam.modules.paper.enums.PaperState;
import com.yf.exam.modules.paper.mapper.PaperMapper;
import com.yf.exam.modules.paper.service.PaperQuAnswerService;
import com.yf.exam.modules.paper.service.PaperQuService;
import com.yf.exam.modules.paper.service.PaperRuleRepoService;
import com.yf.exam.modules.paper.service.PaperRuleService;
import com.yf.exam.modules.paper.service.PaperService;
import com.yf.exam.modules.paper.service.PaperVService;
import com.yf.exam.modules.qu.entity.Qu;
import com.yf.exam.modules.qu.entity.QuAnswer;
import com.yf.exam.modules.qu.enums.QuType;
import com.yf.exam.modules.qu.service.QuAnswerService;
import com.yf.exam.modules.qu.service.QuService;
import com.yf.exam.modules.sys.user.service.SysUserRoleService;
import com.yf.exam.modules.user.service.UserWrongBookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
* <p>
* ???????????? ???????????????
* </p>
*
* @author ????????????
* @since 2020-05-25 16:33
*/
@Service
public class PaperServiceImpl extends ServiceImpl<PaperMapper, Paper> implements PaperService {

    @Autowired
    private SysUserRoleService sysUserRoleService;

    @Autowired
    private PaperRuleService paperRuleService;

    @Autowired
    private ExamService examService;

    @Autowired
    private PaperRuleRepoService paperRuleRepoService;

    @Autowired
    private QuService quService;

    @Autowired
    private QuAnswerService quAnswerService;

    @Autowired
    private PaperService paperService;
    @Autowired
    private PaperVService paperVService;

    @Autowired
    private PaperQuService paperQuService;

    @Autowired
    private PaperQuAnswerService paperQuAnswerService;

    @Autowired
    private UserWrongBookService userWrongBookService;

    /**
     * ??????????????????ABCD??????
     */
    private static List<String> ABC = Arrays.asList(new String[]{
            "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K","L","M","N","O","P","Q","R","S","T","U","V","W","X"
            ,"Y","Z"
    });


    @Override
    public String createPaper(String userId, String examId) {

        // ????????????
        Exam exam = examService.getById(examId);

        if(exam == null){
            throw new ServiceException(1, "??????????????????");
        }

        //????????????
        PaperRule rule = paperRuleService.getById(exam.getRuleId());

        if(rule == null){
            throw new ServiceException(1, "?????????????????????????????????????????????");
        }

        //???????????????????????????
        List<PaperQu> quList = this.generateQuListByRule(rule);

        if(CollectionUtils.isEmpty(quList)){
            throw new ServiceException(1, "???????????????????????????????????????");
        }

        //??????????????????
        String paperId = this.savePaper(userId, examId, rule, quList);

        return paperId;
    }

    @Override
    public ExamDetailRespDTO paperDetail(String paperId) {


        ExamDetailRespDTO respDTO = new ExamDetailRespDTO();

        // ??????????????????
        Paper paper = paperService.getById(paperId);
        BeanMapper.copy(paper, respDTO);

        // ??????????????????
        List<PaperQuDTO> list = paperQuService.listByPaper(paperId);

        List<PaperQuDTO> radioList = new ArrayList<>();
        List<PaperQuDTO> multiList = new ArrayList<>();
        for(PaperQuDTO item: list){
            if(QuType.RADIO.equals(item.getQuType())){
                radioList.add(item);
            }
            if(QuType.MULTI.equals(item.getQuType())){
                multiList.add(item);
            }
        }

        respDTO.setRadioList(radioList);
        respDTO.setMultiList(multiList);
        return respDTO;
    }

    @Override
    public ExamResultRespDTO paperResult(String paperId) {

        ExamResultRespDTO respDTO = new ExamResultRespDTO();

        // ??????????????????
        PaperV paper = paperVService.getById(paperId);
        BeanMapper.copy(paper, respDTO);

        List<PaperQuDetailDTO> quList = paperQuService.listForPaperResult(paperId);
        respDTO.setQuList(quList);

        return respDTO;
    }

    @Override
    public PaperQuDetailDTO findQuDetail(String paperId, String quId) {

        PaperQuDetailDTO respDTO = new PaperQuDetailDTO();
        // ??????
        Qu qu = quService.getById(quId);

        // ????????????
        PaperQu paperQu = paperQuService.findByKey(paperId, quId);
        BeanMapper.copy(paperQu, respDTO);
        respDTO.setContent(qu.getContent());
        respDTO.setImage(qu.getImage());
        respDTO.setVideo(qu.getVideo());

        // ????????????
        List<PaperQuAnswerExtDTO> list = paperQuAnswerService.listForExam(paperId, quId);
        respDTO.setAnswerList(list);

        return respDTO;
    }


    /**
     * ????????????????????????????????????
     * @param rule
     * @return
     */
    private List<PaperQu> generateQuListByRule(PaperRule rule){

        // ???????????????????????????
        List<PaperRuleRepoDTO> list = paperRuleRepoService.listByRule(rule.getId());

        //?????????????????????
        List<PaperQu> quList = new ArrayList<>();

        //??????ID?????????????????????
        List<String> excludes = new ArrayList<>();
        excludes.add("none");

        if (!CollectionUtils.isEmpty(list)) {
            for (PaperRuleRepoDTO item : list) {

                // ?????????
                if(item.getRadioCount() > 0){
                    List<Qu> radioList = quService.listByRandom(item.getRepoId(), QuType.RADIO, excludes, item.getRadioCount());
                    for (Qu qu : radioList) {
                        PaperQu paperQu = this.processPaperQu(item, qu);
                        quList.add(paperQu);
                        excludes.add(qu.getId());
                    }
                }

                //?????????
                if(item.getMultiCount() > 0) {
                    List<Qu> multiList = quService.listByRandom(item.getRepoId(), QuType.MULTI, excludes, item.getMultiCount());
                    for (Qu qu : multiList) {
                        PaperQu paperQu = this.processPaperQu(item, qu);
                        quList.add(paperQu);
                        excludes.add(qu.getId());
                    }
                }

            }
        }
        return quList;
    }

    /**
     * ????????????????????????
     * @param repo
     * @param qu
     * @return
     */
    private PaperQu processPaperQu(PaperRuleRepoDTO repo, Qu qu) {

        //??????????????????
        PaperQu paperQu = new PaperQu();
        paperQu.setQuId(qu.getId());
        paperQu.setAnswered(false);
        paperQu.setIsRight(false);
        paperQu.setQuType(qu.getQuType());

        if (QuType.RADIO.equals(qu.getQuType())) {
            paperQu.setScore(repo.getRadioScore());
            paperQu.setActualScore(repo.getRadioScore());
        }

        if (QuType.MULTI.equals(qu.getQuType())) {
            paperQu.setScore(repo.getMultiScore());
            paperQu.setActualScore(repo.getMultiScore());
        }


        return paperQu;
    }


    /**
     * ????????????
     * @param userId
     * @param rule
     * @param quList
     * @return
     */
    private String savePaper(String userId, String examId, PaperRule rule, List<PaperQu> quList) {

        //????????????????????????
        Paper paper = new Paper();
        paper.setExamId(examId);
        paper.setTitle(rule.getTitle());
        paper.setTotalScore(rule.getTotalScore());
        paper.setTotalTime(rule.getTotalTime());
        paper.setUserScore(0);
        paper.setUserId(userId);
        paper.setCreateTime(new Date());
        paper.setUpdateTime(new Date());
        paper.setQualifyScore(rule.getQualifyScore());
        paper.setState(PaperState.ING);
        paper.setHasSaq(false);

        paperService.save(paper);

        if (!CollectionUtils.isEmpty(quList)) {
            this.savePaperQu(paper.getId(), quList);
        }

        return paper.getId();
    }


    /**
     * ????????????????????????
     * @param paperId
     * @param quList
     */
    private void savePaperQu(String paperId, List<PaperQu> quList){

        List<PaperQu> batchQuList = new ArrayList<>();
        List<PaperQuAnswer> batchAnswerList = new ArrayList<>();

        int sort = 0;
        for (PaperQu item : quList) {

            item.setPaperId(paperId);
            item.setSort(sort);
            item.setId(UUID.randomUUID().toString());

            //????????????
            List<QuAnswer> answerList = quAnswerService.listAnswerByRandom(item.getQuId());

            if (!CollectionUtils.isEmpty(answerList)) {

                int ii = 0;
                for (QuAnswer answer : answerList) {
                    PaperQuAnswer paperQuAnswer = new PaperQuAnswer();
                    paperQuAnswer.setId(UUID.randomUUID().toString());
                    paperQuAnswer.setPaperId(paperId);
                    paperQuAnswer.setQuId(answer.getQuId());
                    paperQuAnswer.setAnswerId(answer.getId());
                    paperQuAnswer.setChecked(false);
                    paperQuAnswer.setSort(ii);
                    paperQuAnswer.setAbc(ABC.get(ii));
                    paperQuAnswer.setIsRight(answer.getIsRight());
                    ii++;
                    batchAnswerList.add(paperQuAnswer);
                }
            }

            batchQuList.add(item);
            sort++;
        }

        //????????????
        paperQuService.saveBatch(batchQuList);

        //????????????????????????
        paperQuAnswerService.saveBatch(batchAnswerList);
    }

    @Override
    public void fillAnswer(PaperAnswerDTO reqDTO) {


        // ?????????
        if(CollectionUtils.isEmpty(reqDTO.getAnswers()) && StringUtils.isBlank(reqDTO.getAnswer())){
            return;
        }

        //??????????????????
        List<PaperQuAnswer> list = paperQuAnswerService.listForFill(reqDTO.getPaperId(), reqDTO.getQuId());

        //????????????
        boolean right = true;

        //??????????????????
        for (PaperQuAnswer item : list) {

            if (reqDTO.getAnswers().contains(item.getId())) {
                item.setChecked(true);
            } else {
                item.setChecked(false);
            }

            //??????????????????????????????
            if (item.getIsRight()!=null && !item.getIsRight().equals(item.getChecked())) {
                right = false;
            }
            paperQuAnswerService.updateById(item);
        }

        //??????????????????
        PaperQu qu = new PaperQu();
        qu.setQuId(reqDTO.getQuId());
        qu.setPaperId(reqDTO.getPaperId());
        qu.setIsRight(right);
        qu.setAnswer(reqDTO.getAnswer());
        qu.setAnswered(true);

        paperQuService.updateByKey(qu);

    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void handExam(String paperId) {

        //??????????????????
        Paper paper = paperService.getById(paperId);

        //????????????????????????????????????
        if(!PaperState.ING.equals(paper.getState())){
            throw new ServiceException(1, "????????????????????????");
        }

        // ?????????
        int objScore = paperQuService.sumObjective(paperId);
        paper.setObjScore(objScore);
        paper.setUserScore(objScore);

        // ???????????????????????????????????????0
        paper.setSubjScore(0);

        // ?????????
        if(paper.getHasSaq()) {
            paper.setState(PaperState.WAIT_OPT);
        }else {
            paper.setState(PaperState.FINISHED);
        }
        paper.setUpdateTime(new Date());

        //??????????????????
        Calendar cl = Calendar.getInstance();
        cl.setTimeInMillis(System.currentTimeMillis());
        int userTime = (int)((System.currentTimeMillis() - paper.getCreateTime().getTime()) / 1000 / 60);
        paper.setUserTime(userTime);

        //????????????
        paperService.updateById(paper);

        //?????????????????????????????????
        List<PaperQuDTO> list = paperQuService.listByPaper(paperId);
        for(PaperQuDTO qu: list){
            // ???????????????????????????????????????
            if(qu.getIsRight().equals(1L)){
                continue;
            }
            //???????????????
            userWrongBookService.addBook(paper.getUserId(), qu.getQuId());
        }
    }

    @Override
    public IPage<PaperPagingRespDTO> paging(PagingReqDTO<PaperDTO> reqDTO) {

        //??????????????????
        Page page = new Page(reqDTO.getCurrent(), reqDTO.getSize());

        // ????????????
        PaperDTO query = reqDTO.getParams();

        // ??????
        String userId = reqDTO.getUserId();

        // ??????????????????????????????
        boolean student = sysUserRoleService.isStudent(userId);
        if(student){
            query.setUserId(userId);
        }

        //????????????
        IPage<PaperPagingRespDTO> pageData = baseMapper.paging(page, reqDTO.getParams());
        return pageData;
     }
}
