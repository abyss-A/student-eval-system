param(
  [string]$TemplatePath = 'd:\桌面\论文\参考资料\附件2-3.重庆第二师范学院全日制本科生毕业论文（或毕业设计）格式（理科版）.docx',
  [string]$OutputPath = 'd:\桌面\测试论文.docx'
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

function Set-CellText {
  param($table, [int]$row, [int]$col, [string]$text)
  $cellRange = $table.Rows.Item($row).Cells.Item($col).Range
  $start = $cellRange.Start
  $end = $cellRange.End - 1
  if ($end -lt $start) { $end = $start }
  $doc.Range($start, $end).Text = $text
}

function Set-ParagraphText {
  param([int]$index, [string]$text)
  $pr = $doc.Paragraphs.Item($index).Range
  $start = $pr.Start
  $end = $pr.End - 1
  if ($end -lt $start) { $end = $start }
  try {
    $doc.Range($start, $end).Text = $text
  } catch {
    $pr.Text = $text
  }
}

function Set-TableCellText {
  param($table, [int]$row, [int]$col, [string]$text)
  $r = $table.Cell($row,$col).Range
  $start = $r.Start
  $end = $r.End - 1
  if ($end -lt $start) { $end = $start }
  $doc.Range($start,$end).Text = $text
}

function New-SystemArchitectureImage {
  param([string]$Path)

  $bmp = New-Object System.Drawing.Bitmap 1600, 900
  $g = [System.Drawing.Graphics]::FromImage($bmp)
  try {
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::ClearTypeGridFit
    $g.Clear([System.Drawing.Color]::White)

    $titleFont = New-Object System.Drawing.Font('Microsoft YaHei', 28, [System.Drawing.FontStyle]::Bold)
    $nodeFont = New-Object System.Drawing.Font('Microsoft YaHei', 16, [System.Drawing.FontStyle]::Regular)
    $pen = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(60,60,60), 3)
    $arrowCap = New-Object System.Drawing.Drawing2D.AdjustableArrowCap(7, 8)
    $pen.CustomEndCap = $arrowCap

    $sf = New-Object System.Drawing.StringFormat
    $sf.Alignment = [System.Drawing.StringAlignment]::Center
    $sf.LineAlignment = [System.Drawing.StringAlignment]::Center

    $g.DrawString('大学生综合测评管理系统总体架构图', $titleFont, [System.Drawing.Brushes]::Black, 800, 70, $sf)

    function Draw-Node([System.Drawing.Graphics]$g, [int]$x, [int]$y, [int]$w, [int]$h, [string]$text, [System.Drawing.Color]$fillColor, $font, $sf) {
      $rect = New-Object System.Drawing.Rectangle($x,$y,$w,$h)
      $rectf = New-Object System.Drawing.RectangleF([single]$x,[single]$y,[single]$w,[single]$h)
      $brush = New-Object System.Drawing.SolidBrush($fillColor)
      $borderPen = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(70,70,70), 2)
      $g.FillRectangle($brush, $rect)
      $g.DrawRectangle($borderPen, $rect)
      $g.DrawString($text, $font, [System.Drawing.Brushes]::Black, $rectf, $sf)
      $brush.Dispose()
      $borderPen.Dispose()
    }

    Draw-Node $g 140 180 1320 95 '用户层：学生端 / 教师端 / 管理员端' ([System.Drawing.Color]::FromArgb(226,239,255)) $nodeFont $sf
    Draw-Node $g 140 320 1320 105 '表现层：Web 前端（Vue3 + Element Plus）' ([System.Drawing.Color]::FromArgb(232,245,233)) $nodeFont $sf
    Draw-Node $g 140 485 1320 120 '业务层：测评管理、加减分审核、统计分析、公告与权限控制' ([System.Drawing.Color]::FromArgb(255,243,224)) $nodeFont $sf
    Draw-Node $g 140 665 1320 105 '数据层：MySQL 数据库 + Redis 缓存（可选）' ([System.Drawing.Color]::FromArgb(248,232,245)) $nodeFont $sf

    $g.DrawLine($pen, 800, 275, 800, 320)
    $g.DrawLine($pen, 800, 425, 800, 485)
    $g.DrawLine($pen, 800, 605, 800, 665)

    $bmp.Save($Path, [System.Drawing.Imaging.ImageFormat]::Png)

    $titleFont.Dispose()
    $nodeFont.Dispose()
    $pen.Dispose()
    $arrowCap.Dispose()
    $sf.Dispose()
  }
  finally {
    $g.Dispose()
    $bmp.Dispose()
  }
}

if (!(Test-Path $TemplatePath)) {
  throw "模板文件不存在: $TemplatePath"
}

$outDir = Split-Path $OutputPath -Parent
if ($outDir -and !(Test-Path $outDir)) {
  New-Item -ItemType Directory -Path $outDir | Out-Null
}

$tempDir = Join-Path $env:TEMP ("thesis-assets-" + [Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $tempDir | Out-Null
$figurePath = Join-Path $tempDir 'figure-2-1-system-architecture.png'

New-SystemArchitectureImage -Path $figurePath
Copy-Item -Path $TemplatePath -Destination $OutputPath -Force

$word = New-Object -ComObject Word.Application
$word.Visible = $false
$word.DisplayAlerts = 0

try {
  $doc = $word.Documents.Open($OutputPath, [ref]$false, [ref]$false)

  # 封面信息（论文封面 + 设计说明封面同步填充，确保模板结构一致）
  $tbl1 = $doc.Tables.Item(1)
  Set-CellText $tbl1 1 2 '大学生综合测评管理系统的设计与实现'
  Set-CellText $tbl1 2 2 '数学与信息工程学院'
  Set-CellText $tbl1 3 2 '计算机科学与技术专业 2022级'
  Set-CellText $tbl1 4 2 '测试学生'
  Set-CellText $tbl1 4 4 '2022001001'
  Set-CellText $tbl1 5 2 '测试导师'
  Set-CellText $tbl1 5 4 '讲师'

  $tbl2 = $doc.Tables.Item(2)
  Set-CellText $tbl2 1 1 '2026'
  Set-CellText $tbl2 1 3 '3'

  $tbl3 = $doc.Tables.Item(3)
  Set-CellText $tbl3 1 2 '大学生综合测评管理系统的设计与实现'
  Set-CellText $tbl3 2 2 '数学与信息工程学院'
  Set-CellText $tbl3 3 2 '计算机科学与技术专业 2022级'
  Set-CellText $tbl3 4 2 '测试学生'
  Set-CellText $tbl3 4 4 '2022001001'
  Set-CellText $tbl3 5 2 '测试导师'
  Set-CellText $tbl3 5 4 '讲师'

  $tbl4 = $doc.Tables.Item(4)
  Set-CellText $tbl4 1 1 '2026'
  Set-CellText $tbl4 1 3 '3'

  # 标题、摘要与关键词
  Set-ParagraphText 82 '大学生综合测评管理系统的设计与实现'
  Set-ParagraphText 83 '数学与信息工程学院  计算机科学与技术专业  2022级  测试学生'
  Set-ParagraphText 84 '摘  要：针对传统大学生综合测评工作中存在的数据分散、审核周期长、统计效率低和结果追溯困难等问题，本文设计并实现了一套大学生综合测评管理系统。系统采用前后端分离架构，前端基于 Vue3 构建交互界面，后端基于 Spring Boot 提供业务服务，数据层采用 MySQL 进行持久化存储。系统围绕“指标配置、数据申报、审核流转、结果统计、可视化分析”构建核心业务流程，实现了测评全过程的标准化与数字化。测试结果表明，该系统在录入效率、审核时效和统计准确率方面均优于人工处理方式，能够有效支撑学院层面的综合测评管理需求。'
  Set-ParagraphText 85 '关键词：综合测评；管理系统；Spring Boot；Vue3；信息化'
  Set-ParagraphText 86 'Abstract: To address issues in traditional comprehensive student evaluation, including scattered data, long review cycles, low statistical efficiency, and weak traceability, this paper designs and implements a comprehensive evaluation management system for university students. The system adopts a front-end/back-end separation architecture, with Vue3 for the user interface, Spring Boot for business services, and MySQL for persistent storage. Core processes include indicator configuration, data submission, review workflow, result statistics, and visual analysis. Experimental results show improvements in input efficiency, review timeliness, and statistical accuracy compared with manual processing. The proposed system effectively supports college-level evaluation management.'
  Set-ParagraphText 87 'Key words: comprehensive evaluation; management system; Spring Boot; Vue3; informatization'

  # 正文章节
  Set-ParagraphText 105 '随着高校育人评价机制不断完善，综合测评已经成为学生评优评奖、党员发展、推免推荐等工作的重要依据。传统纸质或分散表格管理方式存在重复录入、统计误差和责任界定不清等问题，难以满足当前数据化治理需求。'
  Set-ParagraphText 106 '本文以高校日常测评业务为对象，面向学生、辅导员和管理员三类角色，构建一套统一的数据采集与审核平台，以提升综合测评工作效率和管理透明度。'

  Set-ParagraphText 107 '1  绪论'
  Set-ParagraphText 108 '本章围绕研究背景、研究意义以及论文主要工作展开论述，明确系统建设目标与技术路线。'
  Set-ParagraphText 109 '1.1  研究背景'
  Set-ParagraphText 110 '在学工管理场景中，综合测评通常涉及德育、智育、体育、创新实践等多维指标，数据来源广、口径复杂，人工方式难以保证实时性和一致性。'
  Set-ParagraphText 111 '近年来高校信息化建设持续推进，借助 Web 技术构建测评管理平台已成为提升治理能力的可行路径。'
  Set-ParagraphText 112 '1.1.1  研究意义'
  Set-ParagraphText 113 '通过系统化建设可以实现测评数据全流程留痕，降低人为误差，提高审核效率，并为学生成长画像与学院决策提供可靠的数据支撑。'
  Set-ParagraphText 114 '1.2  研究内容与论文结构'
  Set-ParagraphText 115 '本文重点完成了需求分析、系统架构设计、核心功能实现、测试验证及结果分析，并对后续优化方向进行讨论。'

  Set-ParagraphText 116 '2  系统设计与实现'
  Set-ParagraphText 117 '系统采用分层架构设计，覆盖“数据采集—审核流转—统计分析—结果公示”完整链路，支持按学期进行测评方案配置。'
  Set-ParagraphText 118 '2.1  系统架构图'
  Set-ParagraphText 119 '系统按用户层、表现层、业务层和数据层划分，实现前后端解耦与模块化扩展。图2-1 展示了系统总体架构。'

  # 清理原模板在 2.1 区域中的形状对象并插入新图片
  $section2Start = $doc.Paragraphs.Item(118).Range.Start
  $section2End = $doc.Paragraphs.Item(127).Range.Start
  for ($i = $doc.Shapes.Count; $i -ge 1; $i--) {
    $sp = $doc.Shapes.Item($i)
    $anchor = $sp.Anchor.Start
    if ($anchor -ge $section2Start -and $anchor -lt $section2End) {
      $sp.Delete()
    }
  }

  Set-ParagraphText 125 ''
  $imgRange = $doc.Paragraphs.Item(125).Range
  $pic = $doc.InlineShapes.AddPicture($figurePath, $false, $true, $imgRange)
  $pic.LockAspectRatio = -1
  $pic.Width = 360
  $doc.Paragraphs.Item(125).Range.ParagraphFormat.Alignment = 1

  Set-ParagraphText 126 '图2-1  大学生综合测评管理系统总体架构图'

  Set-ParagraphText 127 '2.2  核心功能测试表'
  Set-ParagraphText 128 '为验证系统可用性与稳定性，本文选取典型功能开展测试，结果如表2.1 所示。'
  Set-ParagraphText 129 '表2.1  系统核心功能测试结果'

  Set-ParagraphText 136 '3  系统测试与结果分析'
  Set-ParagraphText 137 '本章从功能正确性、性能表现和用户体验三个维度对系统进行验证。测试环境为 Windows 11、MySQL 8.0，浏览器采用 Chrome 版本 131。'
  Set-ParagraphText 138 '在 300 条测评数据规模下，系统平均页面响应时间控制在 1.2 秒以内；统计模块与人工复核结果保持一致，说明系统能够满足学院日常管理场景。'
  Set-ParagraphText 139 '综合分析表明，系统在保证准确性的前提下显著降低了人工统计工作量。'

  Set-ParagraphText 140 '4  结论与展望'
  Set-ParagraphText 141 '本文完成了大学生综合测评管理系统的设计与实现，形成了从指标配置到结果导出的完整业务闭环。系统在功能完整性与运行稳定性方面达到了预期目标。'
  Set-ParagraphText 142 '后续工作将从两方面推进：一是引入更细粒度的权限与审计机制；二是结合数据挖掘方法开展学生成长趋势分析，提升系统决策支持能力。'
  Set-ParagraphText 143 '通过持续优化，系统可进一步服务于高校精细化管理与教育质量提升。'

  Set-ParagraphText 144 '参考文献'
  Set-ParagraphText 145 '[1] 王伟, 李强. 高校学生综合测评信息化管理研究[J]. 现代教育技术, 2022, 32(6): 88-93.'
  Set-ParagraphText 146 '[2] 张敏. 基于 Spring Boot 的高校管理系统设计与实现[D]. 重庆: 重庆邮电大学, 2023.'
  Set-ParagraphText 147 '[3] Fielding R T. Architectural Styles and the Design of Network-based Software Architectures[D]. University of California, Irvine, 2000.'
  Set-ParagraphText 148 '[4] Sommerville I. Software Engineering[M]. 10th ed. Boston: Pearson, 2016.'
  Set-ParagraphText 149 '[5] 陈鑫, 赵磊. 面向教育场景的数据可视化分析方法[J]. 计算机工程与应用, 2021, 57(18): 150-156.'
  Set-ParagraphText 150 '[6] Oracle Corporation. MySQL 8.0 Reference Manual[EB/OL]. https://dev.mysql.com/doc/, 2025-12-01.'

  Set-ParagraphText 152 '致谢'
  Set-ParagraphText 153 '在论文撰写与系统实现过程中，指导教师在选题、方案设计和论文修改方面给予了细致指导；同学们在测试与反馈阶段提供了大量帮助，在此一并表示诚挚感谢。'
  Set-ParagraphText 154 ''

  Set-ParagraphText 156 '附录'
  Set-ParagraphText 157 '附录1  系统核心数据库表结构（节选）'
  Set-ParagraphText 158 '附录2  关键接口说明（节选）'

  # 删除模板示例表并在原位置创建标准数据表
  $legacyTableRange = $doc.Tables.Item(6).Range
  $legacyTableStart = $legacyTableRange.Start
  $doc.Tables.Item(6).Delete()
  $tableInsertRange = $doc.Range($legacyTableStart, $legacyTableStart)
  $dataTable = $doc.Tables.Add($tableInsertRange, 6, 5)

  $tableData = @(
    @('测试编号','测试模块','测试内容','预期结果','测试结论'),
    @('T01','用户登录','输入正确账号密码登录系统','跳转到个人工作台','通过'),
    @('T02','材料申报','学生提交综合测评证明材料','材料保存并进入待审核','通过'),
    @('T03','审核流程','辅导员审核并给出意见','状态变更为已审核','通过'),
    @('T04','分数统计','按权重计算综合测评分数','分数计算与人工复核一致','通过'),
    @('T05','结果导出','导出班级测评结果 Excel','文件成功下载且字段完整','通过')
  )

  for ($r=1; $r -le 6; $r++) {
    for ($c=1; $c -le 5; $c++) {
      Set-TableCellText $dataTable $r $c $tableData[$r-1][$c-1]
    }
  }

  $dataTable.Range.Font.Name = '宋体'
  $dataTable.Range.Font.Size = 10.5
  $dataTable.Range.ParagraphFormat.Alignment = 1
  $dataTable.Rows.Item(1).Range.Font.Bold = 1

  # 三线表样式：上边线、表头下边线、下边线
  $dataTable.Borders.Enable = 0
  $dataTable.Borders.Item(-1).LineStyle = 1
  $dataTable.Borders.Item(-3).LineStyle = 1
  $dataTable.Rows.Item(1).Borders.Item(-3).LineStyle = 1

  # 更新目录与域
  if ($doc.TablesOfContents.Count -gt 0) {
    $doc.TablesOfContents.Item(1).Update()
    $doc.TablesOfContents.Item(1).UpdatePageNumbers()
  }
  $doc.Fields.Update() | Out-Null

  $doc.Save()
  $doc.Close()
}
finally {
  $word.Quit()
}

Write-Output "Generated: $OutputPath"
Write-Output "Figure: $figurePath"
