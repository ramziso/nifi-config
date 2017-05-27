package com.obs.utils.nifi.service;

import com.github.hermannpencole.nifi.swagger.ApiException;
import com.github.hermannpencole.nifi.swagger.client.FlowApi;
import com.github.hermannpencole.nifi.swagger.client.ProcessGroupsApi;
import com.github.hermannpencole.nifi.swagger.client.TemplatesApi;
import com.github.hermannpencole.nifi.swagger.client.model.*;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Class that offer service for nifi template
 * <p>
 * <p>
 * Created by SFRJ on 01/04/2017.
 */
@Singleton
public class TemplateService {


    /**
     * The logger.
     */
    private final static Logger LOG = LoggerFactory.getLogger(TemplateService.class);

    /**
     * The processGroupService nifi.
     */
    @Inject
    private ProcessGroupService processGroupService;

    @Inject
    private ProcessGroupsApi processGroupsApi;

    @Inject
    private FlowApi flowApi;

    @Inject
    TemplatesApi templatesApi;

    /**
     * @param branch
     * @param fileConfiguration
     * @throws IOException
     * @throws URISyntaxException
     * @throws ApiException
     */
    public void installOnBranch(List<String> branch, String fileConfiguration) throws ApiException {
        ProcessGroupFlowDTO processGroupFlow = processGroupService.createDirectory(branch);
        File file = new File(fileConfiguration);
        String name = FilenameUtils.getBaseName(file.getName());
        //must we force resintall template ?
        /*TemplatesEntity templates = flowApi.getTemplates();
        Optional<TemplateEntity> template = templates.getTemplates().stream().filter(templateParse -> templateParse.getTemplate().getName().equals(name)).findFirst();
        if (!template.isPresent()) {
            template = Optional.of(processGroupsApi.uploadTemplate(processGroupFlow.getId(), file));
        }*/
        Optional<TemplateEntity> template = Optional.of(processGroupsApi.uploadTemplate(processGroupFlow.getId(), file));
        InstantiateTemplateRequestEntity instantiateTemplate = new InstantiateTemplateRequestEntity(); // InstantiateTemplateRequestEntity | The instantiate template request.
        instantiateTemplate.setTemplateId(template.get().getId());
        instantiateTemplate.setOriginX(0d);
        instantiateTemplate.setOriginY(0d);
        processGroupsApi.instantiateTemplate(processGroupFlow.getId(), instantiateTemplate);
    }

    public void undeploy(List<String> branch) throws ApiException {
        Optional<ProcessGroupFlowDTO> processGroupFlow = processGroupService.changeDirectory(branch);
        if (!processGroupFlow.isPresent()) {
            LOG.warn("cannot find " + Arrays.toString(branch.toArray()));
            return;
        }
        ProcessGroupEntity processGroup = processGroupsApi.getProcessGroup(processGroupFlow.get().getId());
        TemplatesEntity templates = flowApi.getTemplates();
        Stream<TemplateEntity> templatesInGroup = templates.getTemplates().stream().filter(templateParse -> templateParse.getTemplate().getGroupId().equals(processGroup.getId()));
        for (TemplateEntity templateInGroup : templatesInGroup.collect(Collectors.toList())) {
            templatesApi.removeTemplate(templateInGroup.getId());
        }
        processGroupsApi.removeProcessGroup(processGroup.getId(),processGroup.getRevision().getVersion().toString(),null);

    }


    }
